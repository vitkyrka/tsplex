#!/usr/bin/python3

import argparse
import sqlite3
import hashlib
import re
import binascii

import genanki
import requests
import requests_cache

requests_cache.install_cache('tsplex-anki.cache')


class SignNote(genanki.Note):

    @property
    def guid(self):
        return genanki.guid_for(self.fields[0])


def get_video_url(id):
    url = 'http://teckensprakslexikon.su.se/ord/%05d' % id
    page = requests.get(url).content.decode('utf-8')
    match = re.search(r'file: "(.*mp4)', page)
    return 'http://teckensprakslexikon.su.se/' + match.groups()[0]


def get_video_data(url):
    data = requests.get(url).content
    return 'data:video/mp4;base64,' + binascii.b2a_base64(data).decode('utf-8').rstrip()


def parse_topics(f):
    items = [line.strip().split(' to ') for line in f.readlines() if ' to ' in line]
    items = [(int(id, 16), topic.strip('",')) for id, topic in items]
    return dict(items)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--db', default='signs.jet')
    parser.add_argument('--topics', default='app/src/main/java/in/rab/tsplex/Topics.kt')
    parser.add_argument('--length', type=int)
    args = parser.parse_args()

    model = genanki.Model(
        1907193319,
        'Sign',
        fields=[
            {'name': 'id'},
            {'name': 'video'},
            {'name': 'word'},
            {'name': 'category'},
            {'name': 'description'},
        ],
        templates=[
            {
                'name': 'Card 1',
                'qfmt': '''

<video id="video" src="{{video}}" width="100%" autoplay loop>
      Din Anki-version verkar inte stödja videouppspelning.
</video>

<p><em>{{category}}</em></p>
<p id="hint"></p>

<script>
window.onload = function() {
  var v = document.getElementById("video");

  var word = '{{description}}'.replace('Bokstaveras: ', '');
  var hint = word.replace(/[^-]/g, ' □ ').replace(/-/g, ' ');
  document.getElementById("hint").innerHTML = hint;
 
  v.addEventListener('click',function() {
    this.playbackRate = this.playbackRate == 1 ? 0.3 : 1;
    this.play();
  }, false);
};
</script>

          ''',
                'afmt': '''
{{FrontSide}}
<hr id="answer">
{{word}}
<br>
<em>{{description}}</em>
''',
            },
        ])

    name = 'TSPLex: Bokstavering'
    if args.length:
      name += ' (%d)' % args.length

    deckid = int(hashlib.sha256(name.encode('utf-8')).hexdigest(), 16) % 10**8
    deck = genanki.Deck(deckid, name)

    with open(args.topics, 'r') as f:
        idtotopic = parse_topics(f)
        idtotopic[0] = ''

    conn = sqlite3.connect(args.db)
    for row in conn.execute('SELECT id, video, sv, topic1, topic2, desc FROM signs WHERE desc LIKE "Bokstaveras%" AND desc NOT LIKE "%//%"'):
        id, video, word, topic1, topic2, desc = row

        topic1 = idtotopic[topic1]
        topic2 = idtotopic[topic2]

        if topic1 == 'Ospecifierad':
            topic1 = ''

        if not topic1:
            category = ''
        elif not topic2:
            category = topic1
        else:
            category = ', '.join([topic1, topic2])

        if 'Delstater i USA' in category:
            continue

        if args.length:
          try:
            chars = desc.split('Bokstaveras: ')[1].split('-')
          except IndexError:
            continue
          if len(chars) != args.length:
            continue

        video = get_video_url(id)
        # video = get_video_data(video)

        print(word, desc, category)

        note = SignNote(model=model, fields=[str(id), video, word, category, desc])
        deck.add_note(note)

    genanki.Package(deck).write_to_file('tsplex.apkg')

if __name__ == '__main__':
    main()
