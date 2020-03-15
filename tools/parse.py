#!/usr/bin/env python3

import argparse
import os
import re
import json
import pickle
import subprocess

import lxml.html

from collections import defaultdict
from multiprocessing import Pool


def parse_list(sign, f, listfile):
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

    variants = [v.strip() for v in root.xpath('//div[contains(@class, "variation")]/a[position()=2]/text()')]
    items.extend([int(x) for x in variants if x and thisid != x])

    return items


def parse_synonyms(sign, f):
    return parse_list(sign, f, 'samma-betydelse.html')


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
    sign['ord'] = [word.lower()]

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
            value = [t.strip() for t in e.itertext() if t.strip() and not t.startswith('[')]
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
                value = next(d.itersiblings()).text.strip()
            except AttributeError:
                continue

        sign[name.lower()] = value

    if len(sign['ämne']) == 0:
        sign['ämne'] = ['Okänt']

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

def fixup_sign(sign):
    if any(topic for topic in sign['ämne'] if 'Orter' in topic or 'Länder' in topic or 'Finland' in topic):
            sign['ord'][0] = sign['ord'][0].capitalize()
    else:
        sign['ord'] = [word if word.isupper() else word.lower() for word in sign['ord']]

    return sign

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cache', action='store_true')
    parser.add_argument('--debug', action='store_true')
    parser.add_argument('files', nargs='+')
    args = parser.parse_args()

    signs = []

    if args.cache:
        try:
            with open('signs.pickle', 'rb') as f:
                signs = pickle.load(f)
        except:
            pass

    if not signs:
        if args.debug:
            signs = []
            for f in args.files:
                print(f"Parsing {f}")
                signs.append(parse_one(f))
        else:
            with Pool(10) as p:
                signs = p.map(parse_one, args.files)

        if args.cache:
            with open('signs.pickle', 'wb') as f:
                pickle.dump(signs, f)

    signs = [fixup_sign(sign) for sign in signs]

    print(json.dumps(signs, indent=2))

if __name__ == '__main__':
    main()
