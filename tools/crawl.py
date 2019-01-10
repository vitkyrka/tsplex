#!/usr/bin/env python3

import argparse
import os
import time
import random
import logging

from urllib.parse import urlparse
from pathlib import Path

import lxml.html

import requests
import requests_cache

base = 'http://teckensprakslexikon.su.se'


def make_throttle_hook():
    def hook(response, *args, **kwargs):
        if not getattr(response, 'from_cache', False):
            delay = random.uniform(0.0, 1.0)
            time.sleep(delay)
        return response
    return hook


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--write', action='store_true')
    parser.add_argument('--last', default=0, type=int)
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG)
    requests_cache.install_cache('requests')

    s = requests_cache.CachedSession()
    s.hooks = {'response': make_throttle_hook()}

    last = args.last
    if not last:
        page = lxml.html.fromstring(s.get(f'{base}/?sortby=fm_id&order=desc').text)
        lastlink = sorted(page.xpath('//a[contains(@href, "/ord/")]/@href'))[-1]
        last = int(lastlink.split('/')[-1])

    basepath = Path(urlparse(base).hostname) / 'ord'

    if args.write:
        try:
            os.makedirs(basepath)
        except FileExistsError:
            pass

    for i in range(1, last+1):
        signid = f'{i:05d}'
        signurl = f'{base}/ord/{signid}'

        resp = s.get(signurl)
        if not resp.ok:
            continue

        page = resp.text
        html = lxml.html.fromstring(page)

        if args.write:
            with open(basepath / f'{signid}.html', 'w') as f:
                f.write(page)

        for url in [url for url in html.xpath('//a/@href') if f'/ord/{signid}' in url and '?' not in url]:
            what = url.split('/')[-1]
            page = s.get(base + url).text

            if args.write:
                try:
                    os.makedirs(basepath / signid)
                except FileExistsError:
                    pass

                with open(basepath / signid / f'{what}.html', 'w') as f:
                    f.write(page)

if __name__ == '__main__':
    main()
