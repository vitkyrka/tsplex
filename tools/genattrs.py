#!/usr/bin/env python3

import argparse
import json
import pprint
import attr
import sqlite3
import os
import re

import jinja2
import logging
import itertools

from genchars import Char
from tagger import Tagger

@attr.s
class Attribute(object):
    name: str = attr.ib()
    tagprefix: str = attr.ib()
    stateprefix: str = attr.ib()
    defaultstate: str = attr.ib(None)
    states = attr.ib(factory=list)
    tagid: int = attr.ib(default=-1)

@attr.s
class AttributeState(object):
    name: str = attr.ib()
    tagid: int = attr.ib()


class AttributeGen(object):
    def __init__(self, chars):
        self.tagger = Tagger(dict([(c.char, c) for c in chars]))
        self.chars = chars
        self.tagmap = {}
        self.tagidx = 1

    def massage_tag(self, t):
        if t.endswith('modifier_medial_contact'):
            return 'action-interaction_type_contact'

        parts = t.split('-')
        if len(parts) > 1 and parts[0] == 'action' and parts[1] in [
                'motion_backward',
                'motion_backward_short',
                'motion_rightward',
                'motion_rightward_short',
                'motion_downward',
                'motion_downward_short',
                'motion_depthwise',
                'motion_leftward',
                'motion_leftward_short',
                'motion_forward',
                'motion_forward_short',
                'motion_upward',
                'motion_upward_short',
                'motion_sideways',
                'motion_vertically',
                ]:
            direction = parts[1].split('_')[1]
            if direction == 'backward':
                direction = 'backwards'
            parts[1] = 'motion_type_moving-modifier_' + direction
            return '-'.join(parts)

        return t

    def massage_tags(self, sign, tags):
        tags = [self.massage_tag(t) for t in tags]

        if sign.hands == 1:
            if sign.left:
                tags.append('hands_two_but_one_active')
            else:
                tags.append('hands_one')
        elif sign.hands == 2:
            tags.append('hands_two')

        if sign.left:
            if sign.left.shape == sign.right.shape:
                tags.extend(['left-handshape_same',
                             'right-handshape_same'])
            if sign.left.pointed == sign.right.pointed:
                tags.extend(['left-attitude_pointed_same',
                             'right-attitude_pointed_same'])
            if sign.left.turned == sign.right.turned:
                tags.extend(['left-attitude_turned_same',
                             'right-attitude_turned_same'])

            symmetric = [
                    ('forward', 'backward'),
                    ('backward', 'forward'),

                    ('leftward', 'rightward'),
                    ('rightward', 'leftward'),

                    ('upward', 'downward'),
                    ('downward', 'upward'),
            ]

            def direction(v):
                return v.split('_')[-1]

            if (direction(sign.left.pointed), direction(sign.right.pointed)) in symmetric:
                tags.extend(['left-attitude_pointed_symmetric',
                             'right-attitude_pointed_symmetric'])

            if (direction(sign.left.turned), direction(sign.right.turned)) in symmetric:
                tags.extend(['left-attitude_turned_symmetric',
                             'right-attitude_turned_symmetric'])


        if not any(t for t in tags if 'position_' in t):
            tags.append('position_unspecified')

        if not any(t for t in tags if 'position-relation' in t):
            tags.append('position-relation_unspecified')

        if any(t for t in tags if 'motion_type_moving' in t):
            tags.append('action-motion_type_moving')

        tags.extend([
            'hands_any',
            'position_any',
            'position-relation_any',
            'right-handshape_any',
            'right-attitude_pointed_any',
            'right-attitude_turned_any',
            'left-handshape_any',
            'left-attitude_pointed_any',
            'left-attitude_turned_any',
        ])

        return sorted(list(set(tags)))

    def tag(self, signs):
        for sign in signs:
            idn = int(sign['id-nummer'])
            s = self.tagger.tag(sign)
            if not s:
                continue

            tags = self.massage_tags(s, s.tags)

            tagids = []
            for t in tags:
                try:
                    v = self.tagmap[t]
                except KeyError:
                    v = self.tagidx
                    self.tagmap[t] = self.tagidx
                    self.tagidx += 1

                tagids.append(v)

            sign['tagids'] = tagids

        return signs

    def gen(self):
        tags = list(self.tagmap.keys())
        transmap = dict([(c.name, c.sv) for c in self.chars])

        transmap.update({
            'hands_one': 'En',
            'hands_two': 'Två, båda aktiva',
            'hands_two_but_one_active': 'Två, en aktiv',
            'position_hand': 'Vänstra handen',
            'relation_unspecified': 'Ospecificerad',
            'position_unspecified': 'Neutralt läge framför kroppen',
            'motion_type_moving': 'förs',

            'hands_any': 'En eller två',
            'position_any': 'Alla',
            'relation_any': 'Alla',
            'handshape_any': 'Alla handformer',
            'attitude_pointed_any': 'Alla riktningar',
            'attitude_turned_any': 'Alla vridningar',

            'handshape_same': '(Samma handform)',
            'attitude_pointed_same': '(Samma riktning)',
            'attitude_turned_same': '(Samma vändning)',
            'attitude_pointed_symmetric': '(Symmetrisk riktning)',
            'attitude_turned_symmetric': '(Symmetrisk vändning)',

            'hands': 'Händer',
            'position': 'Läge',
            'position_relation': 'Lägesrelation',
            'right': 'Höger',
            'left': 'Vänster',
        })

        attribs = [
            Attribute(name='hands',
                      defaultstate='hands_any',
                      tagprefix='hands_',
                      stateprefix=''),
            Attribute(name='position',
                      defaultstate='position_any',
                      tagprefix='position_',
                      stateprefix=''),
            Attribute(name='position_relation',
                      defaultstate='relation_any',
                      tagprefix='position-relation',
                      stateprefix='position-'),
            Attribute(name='right',
                      defaultstate='handshape_any',
                      tagprefix='right-handshape',
                      stateprefix='right-'),
            Attribute(name='right',
                      defaultstate='attitude_pointed_any',
                      tagprefix='right-attitude_pointed',
                      stateprefix='right-'),
            Attribute(name='right',
                      defaultstate='attitude_turned_any',
                      tagprefix='right-attitude_turned',
                      stateprefix='right-'),
            Attribute(name='left',
                      defaultstate='handshape_any',
                      tagprefix='left-handshape',
                      stateprefix='left-'),
            Attribute(name='left',
                      defaultstate='attitude_pointed_any',
                      tagprefix='left-attitude_pointed',
                      stateprefix='left-'),
            Attribute(name='left',
                      defaultstate='attitude_turned_any',
                      tagprefix='left-attitude_turned',
                      stateprefix='left-'),
        ]

        actionattribs = []
        for t in tags:
            if not t.startswith('action-'):
                continue

            parts = t.split('-')
            if len(parts) > 2:
                continue

            action = parts[1]
            actionattribs.append(Attribute(name=action,
                                           tagprefix=t,
                                           stateprefix=action))

        actionattribs = sorted(actionattribs, key=lambda a:transmap[a.name])
        attribs += actionattribs

        for attrib in attribs:
            for t in [t for t in tags if t.startswith(attrib.tagprefix)]:
                tags.remove(t)

                statename = t.replace('action-', '').replace(attrib.stateprefix, '').lstrip('-')
                if not statename or 'any' in statename:
                    assert attrib.tagid == -1
                    attrib.tagid = self.tagmap[t]
                    continue

                state = AttributeState(name=statename, tagid=self.tagmap[t])
                attrib.states.append(state)

            attrib.states = sorted(attrib.states, key=lambda s:transmap[s.name])


        assert not any(a for a in attribs if a.tagid == -1)
        assert not tags

        env = jinja2.Environment(trim_blocks=False, lstrip_blocks=True,
                                 undefined=jinja2.StrictUndefined)
        template = env.from_string('''
// Auto-generated.  Do not edit.

package `in`.rab.tsplex

object Attributes {
    val attributes = arrayOf(
    {% for attr in attribs %}
        Attribute(
            name = "{{ transmap[attr.name] }}",
            {% if attr.defaultstate %}
            defaultStateName = "{{ transmap[attr.defaultstate] }}",
            {% endif %}
            tagId = {{ attr.tagid }},
            states = arrayOf(
            {% for state in attr.states %}
                AttributeState(
                    name = "{{ transmap[state.name] }}",
                    tagId = {{ state.tagid }}
                ){% if not loop.last %},{% endif %}
            {% endfor %}
            )
        ){% if not loop.last %},{% endif %}
    {% endfor %}
    )
}
'''.lstrip())

        return template.render(attribs=attribs, transmap=transmap)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--signs', default='signs.json')
    parser.add_argument('--chars', default='chars.json')
    parser.add_argument('-v', '--verbose', action='store_true')
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO)

    with open(args.signs, 'r') as f:
        signs = json.load(f)

    with open(args.chars, 'r') as f:
        chars = [Char(**o) for o in json.load(f)]

    attgen = AttributeGen(chars)
    attgen.tag(signs)

    pprint.pprint(sorted(attgen.tagmap.items(), key=lambda t:t[0]))
    print(attgen.gen())

if __name__ == '__main__':
    main()
