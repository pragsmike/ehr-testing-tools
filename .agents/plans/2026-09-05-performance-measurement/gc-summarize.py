#!/usr/bin/env python3
"""Summarise one `-Xlog:gc*` log into the two figures the live-set
question actually needs, and a decimated series small enough to commit.

PEAK USED HEAP is the largest pre-collection occupancy anywhere in the
run -- the number a `MaxHeapSize` has to be bigger than. THE LIVE SET
is the POST-collection occupancy, and it is the one that says whether
the run's memory grows with the event count or merely churns: a run
that allocates hard but retains little has a high peak and a flat
post-GC floor, and a run whose world carries its own history has a
post-GC floor that climbs.

Usage: gc-summarize.py <gclog> <out.tsv>
"""
import re, sys

src, dst = sys.argv[1], sys.argv[2]

# "[12.345s][info][gc] GC(17) Pause Young (Normal) (G1 Evacuation Pause) 2048M->512M(3900M) 34.567ms"
# The "(G1 Evacuation Pause)" in a real line carries a DIGIT, so a
# kind sub-pattern that refuses digits cannot reach the sizes past it.
# kind is therefore lazy-any, and the first <n>M-><n>M(<n>M) triple on the
# line is the collection own before/after/capacity reading.
PAUSE = re.compile(
    r'\[(?P<up>[\d.]+)s\].*?GC\((?P<id>\d+)\)\s+(?P<kind>Pause .*?)'
    r'(?P<pre>\d+)M->(?P<post>\d+)M\((?P<cap>\d+)M\)')

rows = []
for line in open(src, errors='replace'):
    m = PAUSE.search(line)
    if not m:
        continue
    rows.append((float(m.group('up')), int(m.group('id')),
                 int(m.group('pre')), int(m.group('post')), int(m.group('cap')),
                 'Full' if 'Full' in m.group('kind') else
                 ('Mixed' if 'Mixed' in m.group('kind') else 'Young')))

if not rows:
    print('NO PAUSE LINES PARSED -- check the log format', file=sys.stderr)
    sys.exit(1)

peak_pre = max(r[2] for r in rows)
peak_post = max(r[3] for r in rows)
fulls = [r for r in rows if r[5] == 'Full']
cap = rows[-1][4]

# The live-set floor's SHAPE. Fit post-GC occupancy against uptime over
# the second half of the run -- the first half is dominated by start-up
# and by the heap growing into its own committed size, which is not the
# question. Reported as first/last and as a least-squares slope so the
# claim "grows linearly with events" has a number under it rather than
# an impression of one.
half = rows[len(rows) // 2:]
n = len(half)
sx = sum(r[0] for r in half); sy = sum(r[3] for r in half)
sxx = sum(r[0] * r[0] for r in half); sxy = sum(r[0] * r[3] for r in half)
den = n * sxx - sx * sx
slope = (n * sxy - sx * sy) / den if den else 0.0

with open(dst, 'w') as f:
    f.write('# uptime_s\tgc_id\tpre_MB\tpost_MB\tcap_MB\tkind\n')
    # Decimate to ~200 rows: a committed artifact, not a raw log.
    step = max(1, len(rows) // 200)
    for r in rows[::step]:
        f.write('%.3f\t%d\t%d\t%d\t%d\t%s\n' % r)

print('collections           : %d (%d Full, %d Mixed)'
      % (len(rows), len(fulls), sum(1 for r in rows if r[5] == 'Mixed')))
print('final heap capacity   : %d MB' % cap)
print('peak PRE-GC used      : %d MB' % peak_pre)
print('peak POST-GC used     : %d MB' % peak_post)
if fulls:
    print('post-FULL-GC used     : %s MB (min %d, max %d)'
          % (','.join(str(r[3]) for r in fulls[:8]),
             min(r[3] for r in fulls), max(r[3] for r in fulls)))
else:
    print('post-FULL-GC used     : NO FULL GC OCCURRED -- the post-Young/Mixed')
    print('                        floor below is the only live-set evidence')
print('post-GC floor, 2nd half: %d MB -> %d MB over %.0f s, slope %.2f MB/s'
      % (half[0][3], half[-1][3], half[-1][0] - half[0][0], slope))
print('series written to     : %s (%d rows, decimated from %d)'
      % (dst, len(rows[::step]), len(rows)))
