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

    return sign

def fixup_sign(sign):
    if any(topic for topic in sign['ämne'] if 'Orter' in topic or 'Länder' in topic or 'Finland' in topic):
            sign['ord'][0] = sign['ord'][0].capitalize()
    else:
        sign['ord'] = [word if word.isupper() else word.lower() for word in sign['ord']]

    return sign

def get_topic_ids(signs):
    topicids = []
    alltopics = {}

    for sign in signs:
        for topic in sign['ämne']:
            base = alltopics
            parts = topic.split(' / ')
            for i, topic in enumerate(parts):
                name = ' » '.join(parts[:i + 1])
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

    signs = [fixup_sign(sign) for sign in signs]

    if args.dump:
        import pprint
        pprint.pprint(signs)
        return

    topicids = get_topic_ids(signs)
    with open('Topics.kt', 'w+') as f:
        f.write('// Auto-generated.  Do not edit.\n\n')
        f.write('package `in`.rab.tsplex\n\n')
        f.write("object Topics {\n")
        f.write("    val names = mapOf(\n")
        _, lastid = topicids[-1]
        f.write('\n'.join(['        0x%08x to "%s"%s' % (id, topic, '' if id == lastid else ',') for topic, id in topicids]))
        f.write("\n    )\n")
        f.write("    val topics = arrayListOf(\n")
        f.write(',\n'.join(['        Topic(0x%08x, "%s")' % (id, topic.split('» ')[-1]) for topic, id in topicids]))
        f.write("\n    )\n")
        f.write("}\n")

    topictoid = dict(topicids)
    with open('topictoid.pickle', 'wb') as f:
        pickle.dump(topictoid, f)

    try:
        os.remove(args.db)
    except:
        pass

    version = 35

    conn = sqlite3.connect(args.db)

    conn.execute("PRAGMA user_version = %d" % version)
    conn.execute("CREATE TABLE signs (id INTEGER, sv TEXT, video TEXT, slug TEXT, transcription TEXT, images INT, desc TEXT, topic1 INT, topic2 INT, comment TEXT, num_examples INT)")
    conn.execute("CREATE TABLE examples (video TEXT UNIQUE, desc TEXT, signid INTEGER)")
    conn.execute("CREATE TABLE synonyms (id INTEGER, otherid INTEGER)")
    conn.execute("CREATE TABLE homonyms (id INTEGER, otherid INTEGER)")

    conn.execute("CREATE TABLE examples_signs (exampleid INTEGER, signid INTEGER)")

    conn.execute("CREATE TABLE sentences_examples (exampleid INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE sentences USING fts4()")

    conn.execute("CREATE TABLE words_signs (signid INTEGER, len INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE words USING fts4()")

    conn.execute("CREATE TABLE descsegs_signs (signid INTEGER, pos INTEGER, len INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE descsegs USING fts4()")

    conn.execute("CREATE TABLE history (id INTEGER, date INTEGER, UNIQUE (id) ON CONFLICT REPLACE)")
    conn.execute("CREATE TABLE favorites (id INTEGER UNIQUE, date INTEGER)")

    conn.execute("CREATE TABLE android_metadata (locale TEXT DEFAULT en_US)")
    conn.execute("INSERT INTO android_metadata VALUES ('en_US')")

    conn.execute("COMMIT")
    conn.execute("BEGIN")

    for sign in signs:
        thisid = int(sign['id-nummer'])

        conn.execute("insert into signs values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     (thisid, ', '.join(sign['ord']),
                      sign['video'],
                      sign['slug'],
                      sign['transkription'],
                      len(sign['images']),
                      sign['beskrivning'],
                      topictoid[sign['ämne'][0].replace('/', '»')],
                      topictoid[sign['ämne'][1].replace('/', '»')] if len(sign['ämne']) > 1 else 0,
                      sign['kommentar'],
                      len(sign['examples'])))

        for word in sign['ord']:
            conn.execute("insert into words values (?)", (word.lower(),))
            conn.execute("insert into words_signs values (?, ?)", (thisid, len(word)))

        for pos, segment in enumerate(sign['beskrivning'].split('//')):
            segment = segment.strip()

            conn.execute("insert into descsegs values (?)", (segment.lower(),))
            conn.execute("insert into descsegs_signs values (?, ?, ?)", (thisid, pos, len(segment)))

        if sign['examples']:
            for vid, desc in sign['examples']:
                newex = True

                mainsignid = int(re.search('-([0-9]{5})-', vid).group(1))

                try:
                    conn.execute("insert into examples values (?, ?, ?)",
                                 (vid, desc, mainsignid))
                except sqlite3.IntegrityError:
                    newex = False

                cursor = conn.execute("select rowid from examples where video = ?", (vid,))
                exampleid = cursor.fetchone()[0]
                conn.execute("insert into examples_signs values (?, ?)", (exampleid, thisid))

                if newex:
                    conn.execute("insert into sentences values (?)", (desc.lower(),))
                    conn.execute("insert into sentences_examples values (?)", (exampleid,))

        conn.executemany("insert into synonyms values (?, ?)",
                         ((thisid, otherid) for otherid in sign['samma-betydelse']))

        # Homonym information seems to be incorrect for Österberg 1916 signs.
        # All the Österberg signs are listed as homonyms of each other.
        if sign['ämne'][0].startswith('Österberg'):
            continue

        conn.executemany("insert into homonyms values (?, ?)",
                         ((thisid, otherid) for otherid in sign['kan-aven-betyda']))

    conn.execute("CREATE INDEX signs_index ON signs(id)")
    conn.execute("CREATE INDEX examples_example_index ON examples_signs(exampleid)")
    conn.execute("CREATE INDEX examples_sign_index ON examples_signs(signid)")
    conn.execute("CREATE INDEX synonyms_index ON synonyms(id)")
    conn.execute("CREATE INDEX homonyms_index ON homonyms(id)")

    conn.execute("INSERT INTO words(words) VALUES (?)", ('optimize',))
    conn.execute("INSERT INTO sentences(sentences) VALUES (?)", ('optimize',))
    conn.execute("INSERT INTO descsegs(descsegs) VALUES (?)", ('optimize',))
    conn.execute("ANALYZE")

    conn.commit()
    conn.close()

    print("PRAGMA user_version = %d;" % version)
    print(subprocess.check_output(['sqlite3', args.db, '.dump']).decode('utf-8'))

if __name__ == '__main__':
    main()
