#!/usr/bin/env python3
"""Regression gates for the launch curriculum's safety and evidence boundaries.

Checks authored data against explicitly reviewed decisions. This is not medical
review, source freshness verification, translation approval or device testing.
"""
import argparse
from collections import Counter
from datetime import date
import json
from pathlib import Path
import re
import sys
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
PRIMARY_HOSTS = {'cpr.heart.org', 'www.sja.org.uk', 'www.nhs.uk', 'www.resus.org.uk',
                 'www.usfa.fema.gov', 'www.london-fire.gov.uk', 'dfs.delhi.gov.in', '112.gov.in', 'www.osha.gov', 'www.acs.org'}
# Each row preserves a meaningful clinical/fire decision, not merely JSON shape.
DECISION_CONTRACTS = {
    ('adult-collapse', 'breathing'): ('not normal breathing', 'emergency'),
    ('adult-collapse', 'cpr'): ('start', 'dispatcher'),
    ('adult-collapse', 'landmark'): ('manikin', 'assess'),
    ('adult-collapse', 'clear'): ('nobody touches', 'prompts'),
    ('school-help', 'limits'): ('no;', 'manikin'),
    ('escape-first', 'role'): ('no;', 'leave'),
    ('escape-first', 'assembly'): ('do not re-enter',),
    ('extinguisher-check', 'fault'): ('do not use', 'report'),
    ('extinguisher-check', 'gauge'): ('unavailable', 'report'),
    ('extinguisher-check', 'change'): ('stop', 'evacuate'),
    ('bleeding-control', 'pressure'): ('direct pressure', 'emergency help'),
    ('bleeding-control', 'embedded'): ('leave it', 'beside', 'not onto'),
    ('thermal-burn', 'cool'): ('cool running water', '20 minutes'),
    ('thermal-burn', 'stuck'): ('leave stuck',),
    ('thermal-burn', 'escalate'): ('urgent medical help',),
    ('recovery-monitor', 'recognise'): ('airway', 'recovery position', 'monitor'),
    ('recovery-monitor', 'monitor'): ('gasping', 'abnormal', 'cpr'),
    ('recovery-monitor', 'trauma'): ('minimise movement', 'airway'),
    ('adult-choking', 'cough'): ('coughing', 'watch'),
    ('adult-choking', 'severe'): ('emergency help', 'five back blows', 'five abdominal thrusts'),
    ('adult-choking', 'sweep'): ('no',),
    ('adult-choking', 'collapse'): ('cpr', 'emergency help'),
    ('adult-choking', 'aftercare'): ('medical evaluation',),
    ('seizure-safety', 'space'): ('hazards', 'cushion'),
    ('seizure-safety', 'mouth'): ('nothing in the mouth',),
    ('seizure-safety', 'call'): ('now', 'emergency'),
    ('fracture-support', 'support'): ('supported', 'medical care'),
    ('fracture-support', 'urgent'): ('emergency medical help',),
    ('agent-choice', 'oil'): ('get out', 'do not move'),
    ('agent-choice', 'electrical'): ('keep clear', 'evacuate'),
    ('agent-choice', 'labels'): ('approved label', 'site procedure'),
    ('agent-choice', 'transfer'): ('evacuate', 'uncertainty'),
    ('school-fire-help', 'count'): ('tell the teacher', 'stay out'),
    ('fainting-response', 'not-faint'): ('no;', 'emergency'),
    ('fainting-response', 'red-flags'): ('emergency', 'now'),
    ('heat-illness', 'change'): ('emergency', 'now', 'cooling'),
    ('heat-illness', 'swallow'): ('no;', 'breathing'),
    ('sprain-support', 'urgent'): ('emergency', 'without straightening'),
    ('minor-wound-care', 'object'): ('do not pull', 'urgent'),
    ('minor-wound-care', 'followup'): ('medical assessment',),
    ('school-injury-help', 'space'): ('teacher', 'clear'),
    ('laboratory-evacuation', 'unknown'): ('instructor', 'evacuate'),
    ('laboratory-evacuation', 'route'): ('clear alternative', 'not move toward'),
    ('home-kitchen-transfer', 'oil'): ('get out', 'do not move'),
    ('home-kitchen-transfer', 'switch'): ('no;', 'unsafe'),
    ('workplace-handover', 'role'): ('no;', 'evacuate'),
    ('workplace-handover', 'condition'): ('do not use', 'report'),
    ('workplace-handover', 'label'): ('no;', 'unknown'),
    ('workplace-handover', 'handover'): ('missing-person', 'stay out'),
}
PART_CONTRACTS = {'FMA7485': 'sternum', 'FMA7309': 'right lung', 'FMA7310': 'left lung',
                  'FMA7394': 'trachea', 'FMA50801': 'brain'}


def validate(content):
    catalog = json.loads((content / 'learning/catalog.json').read_text())
    curriculum = json.loads((content / 'learning/scenarios.json').read_text())
    manifest = json.loads((content / 'anatomy/manifest.json').read_text())
    checks = []
    def check(ok, label):
        if not ok:
            raise ValueError(label)
        checks.append(label)
    scenarios = curriculum['scenarios']
    by_id = {s['id']: s for s in scenarios}
    check(len(by_id) == len(scenarios), 'Unique scenario IDs')
    check(curriculum['review']['status'] == 'draft' and curriculum['review']['reviewer'] is None,
          'No invented clinical approval')
    check(all(s['reviewStatus'] == 'draft' and s['reviewer'] is None for s in scenarios),
          'Every scenario retains draft review status')
    check(all(s['audience'] in {'adult', 'awareness'} for s in scenarios), 'Explicit audience boundaries')
    check(all(s['casualtyAge'] == 'adult' for s in scenarios if s['course'] == 'first-aid'),
          'Adult anatomy never relabelled as paediatric training')
    check(all(not s['practicalAssessment'] and s['evidenceType'] == 'authored-decision-practice' for s in scenarios),
          'Decision evidence does not assert assessed physical skill')
    check(all(s['locale'] == 'en' and s['localisationStatus']['hi'] == 'pending-review' and s['localisationStatus']['sat'] == 'pending-review' for s in scenarios),
          'Unreviewed medical translations are not marked complete')
    check(catalog['contentVersion'] == curriculum['version'], 'Catalogue matches curriculum version')
    check(bool(curriculum['plannedChapters']), 'Remaining chapters explicitly recorded')
    for course in catalog['active']:
        members = [s['id'] for s in scenarios if s['course'] == course['id']]
        awareness = [s['id'] for s in scenarios if s['course'] == course['id'] and s['audience'] == 'awareness']
        check(set(members) == set(course['scenarios']), f'{course["id"]}: exact scenario membership')
        check(set(awareness) == set(course['awarenessScenarios']), f'{course["id"]}: awareness subset excludes adult interventions')
        check(set(members) == set(course['adultScenarios']), f'{course["id"]}: adult path retains awareness content')
    for version in catalog.get('archivedVersions', []):
        archive = json.loads((content / 'learning/archive' / (version + '.json')).read_text())
        check(archive['version'] == version and version != curriculum['version'], f'{version}: original archive is separate')
    for scene in scenarios:
        check(all(target in by_id and by_id[target]['course'] == scene['course'] for target in scene.get('transferScenarioIds', [])), f'{scene["id"]}: transfer targets exist in same course')
        check(bool(scene.get('teachBack')) and bool(scene.get('contextExamples')), f'{scene["id"]}: reasoning and context are authored')
    indexed_steps = {}
    sources = set()
    for scene in scenarios:
        ids = [step['id'] for step in scene['steps']]
        check(len(ids) == len(set(ids)), f'{scene["id"]}: unique stage IDs')
        for step in scene['steps']:
            label = f'{scene["id"]}/{step["id"]}'
            indexed_steps[(scene['id'], step['id'])] = step
            check(sum(c['correct'] is True for c in step['choices']) == 1, f'{label}: exactly one correct alternative')
            url = urlparse(step['source'])
            check(url.scheme == 'https' and url.hostname in PRIMARY_HOSTS and not url.username,
                  f'{label}: primary source URL')
            sources.add(step['source'])
    check(sources == set(curriculum['review']['checkedAgainst']), 'Source index exactly matches authored stages')
    for key, needles in DECISION_CONTRACTS.items():
        step = indexed_steps[key]
        answer = next(c['text'].lower() for c in step['choices'] if c['correct'])
        check(all(n in answer for n in needles), '/'.join(key) + ': reviewed safety decision retained')
    marked = {key for key, step in indexed_steps.items() if step.get('requiresObservation')}
    check(marked == {('adult-collapse', 'landmark'), ('recovery-monitor', 'anatomy')},
          'Only explicit anatomy-learning stages require model observation')
    check(all(indexed_steps[key].get('part') in PART_CONTRACTS for key in marked),
          'Required observations have a supported target part')
    parts = {p['id']: p for p in manifest['parts']}
    for id, name in PART_CONTRACTS.items():
        check(id in parts and name in parts[id]['name'].lower(), f'{id}: correct named anatomical structure')
    for key, step in indexed_steps.items():
        if step.get('part'):
            check(step['part'] in PART_CONTRACTS, '/'.join(key) + ': supported anatomical reference')
    # Reject accidental imported foreign emergency numbers and medication dosing.
    visible = ' '.join(s['title'] + ' ' + ' '.join(step[k] for step in s['steps'] for k in ('cue', 'question', 'explanation')) for s in scenarios)
    check(not re.search(r'\b(?:call|dial)\s+(?:911|999|111)\b', visible, re.I), 'No imported overseas emergency numbers')
    check(not re.search(r'\b\d+(?:\.\d+)?\s*(?:mg|mcg|micrograms?|milligrams?)\b', visible, re.I), 'No unreviewed medication dose introduced')
    return dict(status='passed', checkedOn=str(date.today()), checks=len(checks),
                scenarios=len(scenarios), stages=len(indexed_steps), decisionContracts=len(DECISION_CONTRACTS),
                byCourse=dict(Counter(s['course'] for s in scenarios)),
                clinicalApproval=False, translationsApproved=False,
                evidenceBoundary='Authored-data regression checks only; no clinical, device, accessibility or practical-skill validation.',
                verified=checks)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--content', type=Path, default=ROOT / 'content')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    try:
        result = validate(args.content)
    except (KeyError, ValueError, OSError, TypeError, StopIteration) as error:
        print('FAIL:', error, file=sys.stderr)
        sys.exit(1)
    output = json.dumps(result, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(output)
    print(output)
