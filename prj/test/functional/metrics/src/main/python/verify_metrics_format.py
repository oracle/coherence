#!/usr/bin/env python3
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

import pathlib
import os
import datetime
from abnf.parser import (
    ABNFGrammarNodeVisitor,
    ABNFGrammarRule,
    Alternation,
    CharValNodeVisitor,
    Concatenation,
    GrammarError,
    Literal,
    LiteralNode,
    Match,
    Node,
    NumValVisitor,
    Option,
    ParseCache,
    ParseError,
    Prose,
    Repeat,
    Repetition,
    Rule,
    next_longest,
    sorted_by_longest_match,
)
from urllib.request import urlopen

class FromFileRule(Rule):
    pass

def fetch_abnf_source():
    print("Fetching ABNF file...")
    with urlopen('https://raw.githubusercontent.com/prometheus/OpenMetrics/refs/heads/main/specification/OpenMetrics.md') as response:
        md = response.read().decode('utf-8')
        abnf = []
        copy = False
        for line in md.split('\n'):
            if "~~~~ abnf" in line:
                copy = True
                continue
            elif "~~~~" in line and copy:
                break
            elif copy:
                abnf.append(line)
        if len(abnf) < 30:
            raise Exception("Failed to extract ABNF grammar from the source document")
        return '\n'.join(abnf)

def test_file(parser, filename):
    print(datetime.datetime.now())
    print(f"Processing file: {filename}")
    metrics = pathlib.Path(filename).read_text()
    parser.parse_all(metrics)
    print(datetime.datetime.now())
    print(f"Parsed successfully: {filename}")

abnf = fetch_abnf_source()
print("Writing ABNF file...")
with open("../../../target/metrics.abnf", "w") as f:
    f.write(abnf)

print("Loading ABNF file...")
path = pathlib.Path("../../../target/metrics.abnf")

print("Creating rule from the file...")
FromFileRule.from_file(path)

print("Creating parser...")
parser = FromFileRule.get("exposition")

files = os.listdir("../../../target/")
print("Searching for metrics files")
matching_files = [f for f in files if f.endswith(".metrics.txt")]
if len(matching_files) == 0:
    raise Exception("Metrics files to check are missing")

for filename in matching_files:
    if filename == "Dot Delimited.metrics.txt":
        # dot delimited metrics don't conform to the specs
        continue
    print(f"Test file {filename}")
    test_file(parser, "../../../target/" + filename)