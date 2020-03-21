import argparse

import attr
import json

import lxml.html


@attr.s
class Char(object):
    char = attr.ib()
    name = attr.ib()
    sv = attr.ib()


def main():
    parser = argparse.ArgumentParser()
    # Path to a downloaded https://raw.githubusercontent.com/zrajm/teckentranskription/master/freesans-swl.html
    parser.add_argument('--font-info')
    args = parser.parse_args()

    with open(args.font_info, 'r') as f:
        # <wbr> without closing tag confuses .text_content()
        fontinfo = f.read().replace('<wbr>', '')

    root = lxml.html.document_fromstring(fontinfo)

    chars = []

    def fixup_sv(sv):
        sv = sv.split(':')[-1]
        return sv.strip()

    for tr in root.xpath('//tr[@class="used"]'):
        uni = chr(int(tr.attrib['id'].replace('u', ''), 16))
        sv = tr.xpath('td[@align="left"]')[0].text_content().strip()
        name = tr.xpath('td[@class="tt"]')[0].text_content().strip()

        chars.append(Char(char=uni, name=name, sv=fixup_sv(sv)))

    print(json.dumps([attr.asdict(c) for c in chars], indent=2))


if __name__ == '__main__':
    main()
