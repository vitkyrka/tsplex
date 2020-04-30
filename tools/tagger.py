#!/usr/bin/env python3

import pprint
import logging
import itertools

import attr

from more_itertools import peekable

@attr.s
class Hand(object):
    shape = attr.ib()
    pointed = attr.ib(default=None)
    turned = attr.ib(default=None)

    @classmethod
    def parse(cls, ops):
        a = next(ops)
        b = next(ops)
        c = next(ops)

        if a.startswith('handshape'):
            assert 'pointed' in b
            assert 'turned' in c
            return cls(shape=a, pointed=b, turned=c)
        else:
            assert 'handshape' in c
            assert 'pointed' in a
            assert 'turned' in b
            return cls(shape=c, pointed=a, turned=b)

    @property
    def tags(self):
        tags = [self.shape]

        if self.pointed:
            tags.append(self.pointed)
        if self.turned:
            tags.append(self.turned)

        return tags

@attr.s
class Position(object):
    position = attr.ib()
    relation = attr.ib(default=None)

    @classmethod
    def parse(cls, ops):
        pos = next(ops)
        assert pos.startswith('position') or pos.startswith('handshape')

        p = cls(pos)

        try:
            r = ops.peek()
        except StopIteration:
            return p

        if r.startswith('relation'):
            p.relation = next(ops)

        return p

    @property
    def tags(self):
        tags = [self.position]
        if self.relation:
            tags.append(f'position-{self.relation}')

        return tags


@attr.s
class Repetition(object):
    @classmethod
    def parse(cls, ops):
        next(ops)
        return cls()

    @property
    def tags(self):
        return ['other_repetition']


@attr.s
class Reshaping(object):
    hand: Hand = attr.ib(default=None)

    @classmethod
    def parse(cls, ops):
        next(ops)
        m = cls()
        m.hand = Hand(next(ops))
        return m

    @property
    def tags(self):
        tags = ['motion_type_reshaping']

        if self.hand:
            tags.extend(['motion_type_reshaping-' + t for t in self.hand.tags])

        return tags

@attr.s
class Interaction(object):
    typ = attr.ib()
    modifier = attr.ib(default=None)

    @classmethod
    def parse(cls, ops):
        m = cls(next(ops))

        try:
            n = ops.peek()
        except StopIteration:
            return m

        if n.startswith('modifier'):
            m.modifier = next(ops)

        return m

    @property
    def tags(self):
        tags = [self.typ]

        if self.modifier:
            tags.append(f'{self.typ}-{self.modifier}')

        return tags

@attr.s
class Motion(object):
    typ = attr.ib()
    modifier = attr.ib(default=None)

    @classmethod
    def parse(cls, ops):
        m = cls(next(ops))

        try:
            n = ops.peek()
        except StopIteration:
            return m

        if n.startswith('modifier'):
            m.modifier = next(ops)

        return m

    @property
    def tags(self):
        tags = [self.typ]

        if self.modifier:
            tags.append(f'{self.typ}-{self.modifier}')

        return tags

@attr.s
class Segment(object):
    hands: int = attr.ib(default=1)
    positions = attr.ib(factory=list)
    left: Hand = attr.ib(default=None)
    right: Hand = attr.ib(default=None)
    actions = attr.ib(factory=list)

    @property
    def tags(self):
        tags = []

        if self.left:
            tags.extend(['left-' + t for t in self.left.tags])
        if self.right:
            tags.extend(['right-' + t for t in self.right.tags])

        if not self.positions:
            tags.append('position_unspecified')
        else:
            for p in self.positions:
                if p:
                    tags.extend(p.tags)
                else:
                    tags.append('position_unspecified')

        if self.actions:
            for a in self.actions:
                tags.extend(['action-' + t for t in a.tags])

        return tags


class Tagger(object):
    def __init__(self, charmap):
        self.charmap = charmap

    def parseonesplithand(self, ops):
        s = Segment()

        logging.debug(f'onesplithand ops: {ops}')

        try:
            artend = next(i for i, op in enumerate(ops) if \
                    'position' not in op and
                    'handshape' not in op and
                    'relation' not in op and
                    'attitude' not in op)
        except StopIteration:
            artend = len(ops)

        artops = ops[:artend]

        logging.debug(f'artend: {artend}')
        logging.debug(f'artops: {artops}')
        hands = sum('handshape' in op for op in artops)
        logging.debug(f'hands: {hands}')

        if artops[0] == 'position_arm_lower':
            # This is the only position which includes attitude information.
            # It also has an optional relation like the other positions.
            assert len(artops) == 6 or len(artops) == 7
            assert hands == 1

            # Skip the forearm's attitude when parsing the right hand.
            handops = peekable(artops[len(artops) - 3:])
            s.right = Hand.parse(handops)

            # We want to make the (left) forearm's attitude available somehow
            # so we add a fake left hand.
            s.left = Hand.parse(peekable(['handshape_none'] + [op for op in artops if 'relation' not in op and 'position' not in op]))
        else:
            handops = peekable(op for op in artops if 'relation' not in op and 'position' not in op)
            if hands > 1:
                s.left = Hand.parse(handops)
            s.right = Hand.parse(handops)

        if ops[0].startswith('position') or (hands > 1 and ops[0].startswith('handshape')):
            s.positions = [Position.parse(peekable(iter(artops)))]

        if ops[0].startswith('handshape'):
            hands = 1

        s.hands = hands

        ops = ops[artend:]
        logging.debug(f'restops: {ops}')

        ops = peekable(iter(ops))
        i = 0
        while True:
            try:
                op = ops.peek()
            except StopIteration:
                break

            logging.debug(op)
            if op.startswith('interaction'):
                s.actions.append(Interaction.parse(ops))
            elif op in ['other_separator_groups']:
                next(ops)
                pass
            elif op in ['other_separator_hands']:
                break
            elif op.startswith('other_repetition'):
                s.actions.append(Repetition.parse(ops))
            elif op == 'motion_type_reshaping':
                s.actions.append(Reshaping.parse(ops))
                continue
            elif op.startswith('motion'):
                s.actions.append(Motion.parse(ops))
                continue
            elif op in ['modifier_medial_contact']:
                # Not sure why this appears on its own
                s.actions.append(Interaction.parse(peekable(iter((['interaction_type_contact'])))))
                next(ops)
                pass
            else:
                assert False

            continue

        return s

    def parse(self, ops):
        logging.debug(ops)

        splithands = [list(group) for key, group in itertools.groupby(ops, lambda op:op == 'other_separator_hands') if not key]
        assert len(splithands) <= 2
        logging.debug(splithands)

        firstsplithand = self.parseonesplithand(splithands[0])
        if len(splithands) == 1:
            s = firstsplithand
            assert len(s.positions) <= 1
            assert s.actions
        else:
            secondsplithand = self.parseonesplithand(splithands[1])

            logging.debug(f'firstsplit: {firstsplithand}')
            logging.debug(f'secondsplit: {secondsplithand}')

            assert firstsplithand.hands == 1
            assert secondsplithand.hands == 1

            assert firstsplithand.left == None
            assert secondsplithand.left == None

            s = Segment(hands=2)

            s.positions.append(firstsplithand.positions[0] if firstsplithand.positions else None)
            s.positions.append(secondsplithand.positions[0] if secondsplithand.positions else None)

            s.left = firstsplithand.right
            s.right = secondsplithand.right

            s.actions.extend(firstsplithand.actions)
            s.actions.extend(secondsplithand.actions)

            logging.debug(f'joined: {s}')

        logging.debug(f'tags: {s.tags}')

        return s

    def tag(self, s):
        idn = int(s['id-nummer'])
        logging.debug(idn)
        logging.debug(s['ord'][0])
        try:
            trans = s['transkription']
        except:
            logging.debug('no trans')
            return None

        try:
            chars = [self.charmap[c] for c in trans if c != '#']
        except KeyError:
            if idn not in [18321, 19304, 19305]:
                raise
            return None

        ops = [c.name for c in chars]
        if not ops:
            return None

        # Broken transcriptions
        if idn in [6966, 17172, 1199, 18548, 13286, 15115,
                   16596, 16608, 17066, 18175, 18744,
                   18797, 19100, 19133]:
            return None

        segs = [list(group) for key, group in itertools.groupby(ops, lambda op:op == 'other_separator_segments') if not key]

        return [self.parse(seg) for seg in segs]
