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
    parser.add_argument('--overwrite', action='store_true')
    parser.add_argument('--tail', action='store_true')
    parser.add_argument('--offline', action='store_true')
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG)
    requests_cache.install_cache('cache')

    s = requests_cache.CachedSession()
    s.hooks = {'response': make_throttle_hook()}

    last = args.last
    if not last:
        url = f'{base}/?sortby=fm_id&order=desc'
        if args.offline:
            resp = s.get(url)
        else:
            s.cache.delete_url(url)
            resp = s.get(url)

        page = lxml.html.fromstring(resp.text)
        lastlink = sorted(page.xpath('//a[contains(@href, "/ord/")]/@href'))[-1]
        last = int(lastlink.split('/')[-1])

    basepath = Path(urlparse(base).hostname) / 'ord'

    if args.write:
        try:
            os.makedirs(basepath)
        except FileExistsError:
            pass

    openflags = 'w' if args.overwrite else 'x'

    for i in range(last, 0, -1):
        signid = f'{i:05d}'
        signurl = f'{base}/ord/{signid}'

        if args.tail and s.cache.has_url(signurl):
            break

        if args.offline and not s.cache.has_url(signurl):
            continue

        resp = s.get(signurl)
        if not resp.ok:
            continue

        page = resp.text
        html = lxml.html.fromstring(page)

        if ((i % 1000) == 0):
            logging.debug(f'Processing {signid}')

        if args.write:
            try:
                with open(basepath / f'{signid}.html', openflags) as f:
                    f.write(page)
            except FileExistsError:
                pass

        for url in [url for url in html.xpath('//a/@href') if f'/ord/{signid}' in url and '?' not in url]:
            what = url.split('/')[-1]
            page = s.get(base + url).text

            if args.write:
                try:
                    os.makedirs(basepath / signid)
                except FileExistsError:
                    pass

                try:
                    with open(basepath / signid / f'{what}.html', openflags) as f:
                        f.write(page)
                except FileExistsError:
                    pass

if __name__ == '__main__':
    main()
