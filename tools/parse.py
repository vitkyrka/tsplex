#!/usr/bin/env python3

import argparse
import os
import re
import json
import pickle
import subprocess

import lxml.html

from functools import partial
from collections import defaultdict
from multiprocessing import Pool


def parse_list(sign, f, listfile, include_variants=False):
    signdir = os.path.splitext(f)[0]
    synonymfile = os.path.join(signdir, listfile)
    if not os.path.exists(synonymfile):
        return []

    x = lxml.html.parse(synonymfile)
    root = x.getroot()

    thisid = sign['id-nummer']
    try:
        items = [int(x) for x in root.xpath('//td[contains(@class, "id")]/a/span/text()') if thisid != x]
    except AttributeError:
        raise Exception(synonymfile)

    if include_variants:
        variants = [v.strip() for v in root.xpath('//div[contains(@class, "variation")]/a[position()=2]/text()')]
        items.extend([int(x) for x in variants if x and thisid != x])

    return items


def parse_synonyms(sign, f):
    return parse_list(sign, f, 'samma-betydelse.html', include_variants=True)


def parse_homonyms(sign, f):
    return parse_list(sign, f, 'kan-aven-betyda.html')


def parse_one(f):
    sign = defaultdict(str)
    x = lxml.html.parse(f)
    root = x.getroot()

    video = root.xpath('//source[@type="video/mp4"]/@src')[0]

    scripts = '\n'.join(root.xpath('//script/text()'))
    examplevids = re.findall('data-video="([^"]+)"', scripts)
    exampledescs = [t.strip() for t in re.findall('<span class="text">([^<]+)</span>', scripts)]

    assert(len(exampledescs) == len(examplevids))

    sign['video'] = video
    sign['examples'] = list(zip(examplevids, exampledescs))

    word = root.xpath('//div[@class="su-infohead"]/h2/text()')[0].strip()
    sign['ord'] = [word]

    try:
        also = root.xpath('//div[@class="su-infohead"]/h2/span[@class="also_means"]/text()')[0].strip()
        sign['ord'] += [w.strip() for w in also.split(',')]
    except IndexError:
        pass

    # <div class="leftcolumn">Beskrivning</div>
    # <div class="rightcolumn">Bokstaveras: A-F-R-O</div>
    for d in root.xpath('//div[@class="leftcolumn"]'):
        name = d.text

        if name == 'Ämne':
            e = next(d.itersiblings())
            # Skip [Även] and [Endast] for now
            value = []

            topic = None
            texts = [t.strip() for t in e.itertext() if t.strip()]
            for t in texts:
                if t.startswith('['):
                    assert topic
                    value.append((topic, t))
                    topic = None
                else:
                    if topic:
                        value.append((topic, ''))
                    topic = ' / '.join([p.capitalize() for p in t.split(' / ')])

            if topic:
                value.append((topic, ''))
        elif name == 'Förekomster':
            occurence = [l.strip().replace(' träffar', '').split(' ') for l in next(d.itersiblings()).text_content().strip().split('\n')]
            totalhits = 0
            try:
                for where, hits in occurence:
                    hits = hits.split(' ', maxsplit=1)[0]
                    sign['förekomster-' + where.lower().rstrip(':')] = int(hits)
                    totalhits += int(hits)
            except ValueError:
                # Percent after enkäter
                pass
            value = totalhits
        else:
            try:
                value = next(d.itersiblings()).text_content().strip()
            except AttributeError:
                continue

        sign[name.lower()] = value

    if len(sign['ämne']) == 0:
        sign['ämne'] = [('Okänt', '')]

    sign['slug'] = re.search('movies/[^/]+/(.*?)-%s' % sign['id-nummer'], sign['video']).groups()[0]
    sign['samma-betydelse'] = parse_synonyms(sign, f)
    sign['kan-aven-betyda'] = parse_homonyms(sign, f)
    sign['images'] = list(root.xpath('//img[@class="img-50"]/@src'))

    if int(sign['id-nummer']) == 1:
        assert len(examplevids) > 3

    try:
        explvid = next(u for u in root.xpath('//source[@type="video/mp4"]/@src') if '-explanation' in u)
        explanation = root.xpath('//div[@class="descriptionText"]')[0].text_content()

        sign['explanation_video'] = explvid
        sign['explanation'] = explanation

    except StopIteration:
        assert int(sign['id-nummer']) != 13464
        pass

    return sign

def fixup(sign):
    # Homonym information seems to be incorrect for Österberg 1916 signs.
    # All the Österberg signs are listed as homonyms of each other.
    if sign['ämne'][0][0].startswith('Österberg'):
        sign['kan-aven-betyda'] = []

    # Pseudo-sign with wrong homonym information
    if int(sign['id-nummer']) == 715:
        sign['kan-aven-betyda'] = []

    return sign

def fixup_relations(sign, signs):
    thisid = int(sign['id-nummer'])
    synonyms = [int(s['id-nummer']) for s in signs if thisid in s['samma-betydelse']]
    if len(synonyms) > len(sign['samma-betydelse']):
        diff = set(synonyms) - set(sign['samma-betydelse'])
        sign['samma-betydelse'].extend(list(diff))

    homonyms = [int(s['id-nummer']) for s in signs if thisid in s['kan-aven-betyda']]
    if len(homonyms) > len(sign['kan-aven-betyda']):
        diff = set(homonyms) - set(sign['kan-aven-betyda'])
        sign['kan-aven-betyda'].extend(list(diff))

    key = f'-{sign["id-nummer"]}-'
    known = [(url, title) for url, title in sign['examples'] if key in url]

    found = set()
    for s in signs:
        found.update([(url, title) for url, title in s['examples'] if key in url])

    if len(found) > len(known):
        founddict = dict(list(found))
        foundurls = set([u for u, title in found])
        knownurls = set([u for u, title in known])

        sign['examples'].extend([(u, founddict[u]) for u in foundurls - knownurls])

    return sign

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cache', action='store_true')
    parser.add_argument('--debug', action='store_true')
    parser.add_argument('files', nargs='+')
    args = parser.parse_args()

    parsed = []
    fixedup = []

    if args.cache:
        try:
            with open('signs-parsed.pickle', 'rb') as f:
                parsed = pickle.load(f)
        except:
            pass

    if parsed:
        if args.cache:
            try:
                with open('signs-fixedup.pickle', 'rb') as f:
                    fixedup = pickle.load(f)
            except:
                pass
    else:
        if args.debug:
            parsed = []
            for f in args.files:
                print(f"Parsing {f}")
                parsed.append(parse_one(f))
        else:
            with Pool(10) as p:
                parsed = p.map(parse_one, args.files)

        if args.cache:
            with open('signs-parsed.pickle', 'wb') as f:
                pickle.dump(parsed, f)

    if not fixedup:
        fixedup = [fixup(sign) for sign in parsed]

        with Pool(10) as p:
            fixedup = p.map(partial(fixup_relations, signs=fixedup), fixedup)

        if args.cache:
            with open('signs-fixedup.pickle', 'wb') as f:
                pickle.dump(parsed, f)

    print(json.dumps(fixedup, indent=2))

if __name__ == '__main__':
    main()
