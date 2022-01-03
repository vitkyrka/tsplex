#!/usr/bin/env python3

import argparse
import os
import time
import random
import logging
import sqlite3
import re

from urllib.parse import urlparse
from pathlib import Path

import lxml.html

import requests

base = 'https://teckensprakslexikon.su.se'


def make_throttle_hook():
    def hook(response, *args, **kwargs):
        delay = random.uniform(0.0, 1.0)
        time.sleep(delay)
        return response
    return hook


class CachedSession:
    def __init__(self, session, db, offline):
        self.session = session
        self.conn = sqlite3.connect(db)
        self.offline = offline

        self.conn.execute("CREATE TABLE IF NOT EXISTS cache (url TEXT, content TEXT, UNIQUE (url) ON CONFLICT REPLACE)")
        self.conn.execute("CREATE INDEX IF NOT EXISTS cache_index ON cache(url)")
        self.conn.commit()
        self.conn.execute("BEGIN")

    def get_from_cache(self, url):
        cur = self.conn.cursor()
        # To reuse cache from before the switch to HTTPS
        url = url.replace("https://", "http://")
        cur.execute("select content from cache where url = ?", (url,))
        row = cur.fetchone()
        cur.close()
        if not row:
            return None
        return row[0]

    def _put_into_cache(self, url, content):
        # See above
        url = url.replace("https://", "http://")
        self.conn.execute("INSERT INTO cache VALUES (?, ?)", (url, content))

    def get(self, url, cached_ok=True):
        content = None
        if cached_ok:
            content = self.get_from_cache(url)
        if content is None:
            if self.offline:
                raise Exception(f"{url} not in cache while in offline mode")
            resp = self.session.get(url)
            if resp.ok:
                content = resp.text
            elif resp.status_code == 404:
                content = ''
            else:
                resp.raise_for_success()

            self._put_into_cache(url, content)

        return content

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--write', action='store_true')
    parser.add_argument('--last', default=0, type=int)
    parser.add_argument('--overwrite', action='store_true')
    parser.add_argument('--tail', action='store_true')
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--db', default='tsplex.sqlite')
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG)

    session = requests.Session()
    session.hooks = {'response': make_throttle_hook()}

    cache = CachedSession(session, args.db, offline=args.offline)

    last = args.last
    if not last:
        url = f'{base}/?sortby=fm_id&order=desc'
        text = cache.get(url, cached_ok=args.offline)
        page = lxml.html.fromstring(text)
        lastlink = sorted(page.xpath('//a[contains(@href, "/ord/")]/@href'))[-1]
        last = int(lastlink.split('/')[-1].split('?')[0])

    logging.debug(f"last: {last}")

    basepath = Path(urlparse(base).hostname) / 'ord'

    if args.write:
        try:
            os.makedirs(basepath)
        except FileExistsError:
            pass

    openflags = 'w' if args.overwrite else 'x'

    try:
        for i in range(last, 0, -1):
            signid = f'{i:05d}'
            signurl = f'{base}/ord/{signid}'

            if args.tail and cache.get_from_cache(signurl) is not None:
                logging.debug(f"{signurl} in cache, stopping --tail run")
                break

            if args.offline and cache.get_from_cache(signurl) is None:
                continue

            page = cache.get(signurl)
            if not page:
                continue

            html = lxml.html.fromstring(page)

            if ((i % 1000) == 0):
                logging.debug(f'Processing {signid}')

            if args.write:
                try:
                    fn = basepath / f'{signid}.html'
                    with open(fn, openflags) as f:
                        f.write(page)
                    logging.debug(f'Wrote {fn}')
                except FileExistsError:
                    pass

            for url in [url for url in html.xpath('//a/@href') if f'/ord/{signid}' in url and '?' not in url]:
                what = url.split('/')[-1]
                page = cache.get(base + url)

                if args.write:
                    try:
                        os.makedirs(basepath / signid)
                    except FileExistsError:
                        pass

                    try:
                        fn = basepath / signid / f'{what}.html'
                        with open(fn, openflags) as f:
                            f.write(page)
                        logging.debug(f'Wrote {fn}')
                    except FileExistsError:
                        pass
    finally:
        cache.conn.commit()
        cache.conn.close()

if __name__ == '__main__':
    main()
