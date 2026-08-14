# Demos

Watch a synthetic hospital run, or read a small captured trace end to
end. Everything here works from a fresh checkout with `bin/ehrt` and
no other setup.

## Scenarios — generate and watch

Runnable, population-scale configurations. Start with
`scenarios/clinic-decade/`: a busy weekday across twenty-odd ailments,
watched on a live bed board at an hour of hospital time per minute.

## Traces — small enough to read whole

Captured runs, ten patients or fewer: the exact command, the resulting
ground truth, and the rendered HL7 messages side by side. Start with
`traces/boarding-transfer/` — ED hallway boarding and a bed-ready
transfer, emergent from census pressure, never scripted.
`traces/site-profiles/` shows the same traffic re-dressed to look like
your hospital's own feed.
