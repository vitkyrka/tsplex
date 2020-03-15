#!/usr/bin/env python3

import argparse
import os
import re
import json
import sqlite3

from collections import defaultdict


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
    parser.add_argument('--signs', default='signs.json')
    parser.add_argument('--topics', default='Topics.kt')
    parser.add_argument('--db', default='signs.db')
    args = parser.parse_args()

    def hook(d):
        dd = defaultdict(str)
        dd.update(d)
        return dd

    with open(args.signs, 'r') as f:
        signs = json.load(f, object_hook=hook)

    topicids = get_topic_ids(signs)
    with open(args.topics, 'w') as f:
        f.write('// Auto-generated.  Do not edit.\n\n')
        f.write('package `in`.rab.tsplex\n\n')
        f.write("object Topics {\n")
        f.write("    val names = mapOf(\n")
        _, lastid = topicids[-1]
        f.write('\n'.join(['        0x%08xL to "%s"%s' % (id, topic, '' if id == lastid else ',') for topic, id in topicids]))
        f.write("\n    )\n")
        f.write("    val topics = arrayListOf(\n")
        f.write(',\n'.join(['        Topic(0x%08x, "%s")' % (id, topic.split('» ')[-1]) for topic, id in topicids]))
        f.write("\n    )\n")
        f.write("}\n")

    topictoid = dict(topicids)

    try:
        os.remove(args.db)
    except:
        pass

    version = 40

    conn = sqlite3.connect(args.db)

    conn.execute("PRAGMA user_version = %d" % version)
    conn.execute("CREATE TABLE signs (id INTEGER, sv TEXT, video TEXT, slug TEXT, transcription TEXT, images INT, desc TEXT, topic1 INT, topic2 INT, comment TEXT, num_examples INT, occurence INT, occurence_lexicon INT, occurence_corpus INT, occurence_surveys INT)")
    conn.execute("CREATE TABLE examples (video TEXT UNIQUE, desc TEXT, signid INTEGER)")
    conn.execute("CREATE TABLE synonyms (id INTEGER, otherid INTEGER)")
    conn.execute("CREATE TABLE homonyms (id INTEGER, otherid INTEGER)")
    conn.execute("CREATE TABLE explanations (id INTEGER, video TEXT, desc TEXT)")

    conn.execute("CREATE TABLE examples_signs (exampleid INTEGER, signid INTEGER)")

    conn.execute("CREATE TABLE sentences_examples (exampleid INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE sentences USING fts4()")

    conn.execute("CREATE TABLE words_signs (signid INTEGER, len INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE words USING fts4()")

    conn.execute("CREATE TABLE descsegs_signs (signid INTEGER, pos INTEGER, len INTEGER)")
    conn.execute("CREATE VIRTUAL TABLE descsegs USING fts4()")

    conn.execute("CREATE TABLE history (id INTEGER, date INTEGER, UNIQUE (id) ON CONFLICT REPLACE)")

    conn.execute("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, lastused INTEGER)")
    conn.execute("CREATE TABLE bookmarks (id INTEGER, date INTEGER, folderid INTEGER, UNIQUE (id) ON CONFLICT REPLACE)")

    conn.execute("CREATE TABLE android_metadata (locale TEXT DEFAULT en_US)")
    conn.execute("INSERT INTO android_metadata VALUES ('en_US')")

    conn.execute("COMMIT")
    conn.execute("BEGIN")

    for sign in signs:
        thisid = int(sign['id-nummer'])

        conn.execute("insert into signs values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     (thisid, ', '.join(sign['ord']),
                      sign['video'],
                      sign['slug'],
                      sign['transkription'],
                      len(sign['images']),
                      sign['beskrivning'],
                      topictoid[sign['ämne'][0].replace('/', '»')],
                      topictoid[sign['ämne'][1].replace('/', '»')] if len(sign['ämne']) > 1 else 0,
                      sign['kommentar'],
                      len(sign['examples']),
                      sign['förekomster'],
                      sign['förekomster-lexikonet'],
                      sign['förekomster-korpusmaterial'],
                      sign['förekomster-enkäter']))

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

        if sign['explanation_video']:
            conn.execute("insert into explanations values (?, ?, ?)",
                    (thisid, sign['explanation_video'], sign['explanation']))

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
    conn.execute("CREATE INDEX explanations_index ON explanations(id)")

    conn.execute("INSERT INTO words(words) VALUES (?)", ('optimize',))
    conn.execute("INSERT INTO sentences(sentences) VALUES (?)", ('optimize',))
    conn.execute("INSERT INTO descsegs(descsegs) VALUES (?)", ('optimize',))
    conn.execute("ANALYZE")

    conn.commit()
    conn.close()

if __name__ == '__main__':
    main()
