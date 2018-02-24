#!/usr/bin/env python3

import argparse
import os
import re
import pickle
import sqlite3
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
        return [int(x) for x in root.xpath('//td[contains(@class, "id")]/a/span/text()') if thisid != x]
    except AttributeError:
        raise Exception(synonymfile)


def parse_synonyms(sign, f):
    return parse_list(sign, f, 'samma-betydelse.html')


def parse_homonyms(sign, f):
    return parse_list(sign, f, 'kan-aven-betyda.html')


def parse_one(f):
    sign = defaultdict(str)
    x = lxml.html.parse(f)
    root = x.getroot()

    scripts = root.xpath('//div[contains(@class, "videodiv")]//script')
    videos = [next(l for l in script.text.splitlines() if '.mp4' in l).strip('" ,').split('"')[1]
              for script in scripts
              if '-slow' not in script.text]

    exampledescs = root.xpath('//ul[@class="video-example-nav"]//span[@class="text"]/text()')
    examplevids = videos[1:]

    assert(len(exampledescs) == len(examplevids))

    sign['video'] = videos[0]
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
            value = [t.strip() for t in e.itertext()]
        else:
            try:
                value = next(d.itersiblings()).text.strip()
            except AttributeError:
                continue

        sign[name.lower()] = value

    if len(sign['ämne']) == 0:
        sign['ämne'] = ['Ospecifierad']

    sign['slug'] = re.search('movies/[^/]+/(.*?)-%s' % sign['id-nummer'], sign['video']).groups()[0]
    sign['samma-betydelse'] = parse_synonyms(sign, f)
    sign['kan-aven-betyda'] = parse_homonyms(sign, f)
    sign['images'] = list(root.xpath('//img[@class="img-50"]/@src'))

    return sign


def get_topic_ids(signs):
    topicids = []
    alltopics = {}

    for sign in signs:
        for topic in sign['ämne']:
            base = alltopics
            parts = topic.split(' / ')
            for i, topic in enumerate(parts):
                name = ' / '.join(parts[:i + 1])
                try:
                    base = base[name]
                except KeyError:
                    base.update({name: {}})
                    base = base[name]

    def assignids(topics, base, level):
        i = 1
        for topic in sorted(topics.keys()):
            id = base + (i << 8 * level)
            topicids.append((topic, id))
            assignids(topics[topic], id, level + 1)
            i += 1

    assignids(alltopics, 0, 0)
    return topicids


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cache', action='store_true')
    parser.add_argument('--dump', action='store_true')
    parser.add_argument('--db', default='signs.db')
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
        with Pool(10) as p:
            signs = p.map(parse_one, args.files)

        if args.cache:
            with open('signs.pickle', 'wb') as f:
                pickle.dump(signs, f)

    if args.dump:
        import pprint
        pprint.pprint(signs)
        return

    topicids = get_topic_ids(signs)
    with open('Topics.kt', 'w+') as f:
        f.write('// Auto-generated.  Do not edit.\n\n')
        f.write("object Topics {\n")
        f.write("	val names = mapOf(\n")
        _, lastid = topicids[-1]
        f.write('\n'.join(['		0x%08x to "%s"%s' % (id, topic, '' if id == lastid else ',') for topic, id in topicids]))
        f.write("\n    )\n")
        f.write("}\n")

    topictoid = dict(topicids)
    with open('topictoid.pickle', 'wb') as f:
        pickle.dump(topictoid, f)

    try:
        os.remove(args.db)
    except:
        pass

    version = 18

    conn = sqlite3.connect(args.db)

    conn.execute("PRAGMA user_version = %d" % version)
    conn.execute("CREATE TABLE signs (id INTEGER, sv TEXT, video TEXT, slug TEXT, images INT, desc TEXT, topic1 INT, topic2 INT, comment TEXT)")
    conn.execute("CREATE TABLE examples (id INTEGER, video TEXT, desc TEXT)")
    conn.execute("CREATE TABLE synonyms (id INTEGER, otherid INTEGER)")
    conn.execute("CREATE TABLE homonyms (id INTEGER, otherid INTEGER)")

    conn.execute("CREATE TABLE history (id INTEGER, date INTEGER, UNIQUE (id) ON CONFLICT REPLACE)")
    conn.execute("CREATE TABLE favorites (id INTEGER UNIQUE, date INTEGER)")

    conn.execute("CREATE TABLE android_metadata (locale TEXT DEFAULT en_US)")
    conn.execute("INSERT INTO android_metadata VALUES ('en_US')")

    conn.execute("BEGIN")

    for sign in signs:
        thisid = int(sign['id-nummer'])

        conn.execute("insert into signs values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     (thisid, ', '.join(sign['ord']),
                      sign['video'],
                      sign['slug'],
                      len(sign['images']),
                      sign['beskrivning'],
                      topictoid[sign['ämne'][0]],
                      topictoid[sign['ämne'][1]] if len(sign['ämne']) > 1 else 0,
                      sign['kommentar']))

        conn.executemany("insert into examples values (?, ?, ?)",
                         ((thisid, vid, desc) for vid, desc in sign['examples']))

        conn.executemany("insert into synonyms values (?, ?)",
                         ((thisid, otherid) for otherid in sign['samma-betydelse']))

        # Homonym information seems to be incorrect for Österberg 1916 signs.
        # All the Österberg signs are listed as homonyms of each other.
        if sign['ämne'][0].startswith('Österberg'):
            continue

        conn.executemany("insert into homonyms values (?, ?)",
                         ((thisid, otherid) for otherid in sign['kan-aven-betyda']))

    conn.execute("CREATE INDEX signs_index ON signs(sv)")
    conn.execute("CREATE INDEX examples_index ON examples(id)")
    conn.execute("CREATE INDEX synonyms_index ON synonyms(id)")
    conn.execute("CREATE INDEX homonyms_index ON homonyms(id)")

    conn.commit()
    conn.close()

    print("PRAGMA user_version = %d;" % version)
    print(subprocess.check_output(['sqlite3', args.db, '.dump']).decode('utf-8'))

if __name__ == '__main__':
    main()
