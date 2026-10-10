"""Render the portable handbook. Requires Python packages markdown and beautifulsoup4.

Run from any directory: python build_rulebook.py
Inputs: RULEBOOK.md and RELEASE-CHECKLIST.md beside this file.
Outputs: index.html, checklist.html, contrast-report.json, verification.json.
"""
from pathlib import Path
from html import escape
import json
import re
import hashlib
import markdown
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parent


def luminance(value):
    rgb = [int(value[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in rgb]
    return sum(c * v for c, v in zip((.2126, .7152, .0722), linear))


def contrast(a, b):
    x, y = sorted((luminance(a), luminance(b)))
    return (y + .05) / (x + .05)


CSS = """
:root{color-scheme:light;--bg:#f4f6f8;--paper:#fff;--text:#0f172a;--muted:#475569;--line:#cbd5e1;--link:#1d4ed8;--soft:#eef2f7;--control:#64748b}
:root[data-theme=dark]{color-scheme:dark;--bg:#0b1220;--paper:#0f172a;--text:#f8fafc;--muted:#cbd5e1;--line:#475569;--link:#93c5fd;--soft:#1e293b;--control:#94a3b8}
*{box-sizing:border-box}html{scroll-padding-top:24px}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.7 -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}
a{color:var(--link);text-underline-offset:3px}a:hover{text-decoration-thickness:2px}a:focus-visible,input:focus-visible,select:focus-visible,summary:focus-visible,.table-scroll:focus-visible{outline:3px solid var(--link);outline-offset:4px}
.skip{position:absolute;top:-100px;left:10px;padding:12px;background:var(--paper);z-index:5}.skip:focus{top:10px}
.layout{display:grid;grid-template-columns:288px minmax(0,1fr);max-width:1600px;margin:auto}
aside{position:sticky;top:0;height:100vh;overflow-y:auto;border-right:1px solid var(--line);padding:28px 20px 36px;background:var(--paper)}
.brand{font-size:13px;font-weight:750;letter-spacing:.12em;text-transform:uppercase;color:var(--muted);margin-bottom:20px}.brand b{display:block;letter-spacing:-.035em;font-size:25px;color:var(--text);text-transform:none}
label{display:block;font-size:13px;font-weight:650;margin:12px 0 5px}input,select{font:inherit;font-size:15px;width:100%;min-height:44px;border:1px solid var(--control);border-radius:7px;padding:8px 10px;background:var(--paper);color:var(--text)}
.nav-links{display:flex;flex-wrap:wrap;gap:14px;margin:18px 0;font-size:14px}.nav-links a{padding:7px 0}.toc ul{list-style:none;padding:0;margin:0}.toc li{margin:3px 0}.toc a{display:block;text-decoration:none;color:var(--muted);font-size:13px;line-height:1.45;padding:8px;border-radius:5px}.toc a:hover{background:var(--soft);color:var(--text)}.toc ul ul{display:none}
details>summary{font-weight:700;cursor:pointer;min-height:44px;padding:9px 0}main{min-width:0;padding:38px 48px 72px;max-width:1170px}
.hero{padding:40px 44px;background:#0f172a;color:#f8fafc;border-radius:16px;margin-bottom:28px}.eyebrow{font-size:12px;text-transform:uppercase;letter-spacing:.15em;color:#93c5fd;font-weight:700}.hero h1{font-size:clamp(32px,4vw,54px);line-height:1.08;letter-spacing:-.045em;margin:16px 0 20px;max-width:680px}.hero p{color:#cbd5e1;max-width:620px;font-size:17px}.stats{display:flex;flex-wrap:wrap;gap:14px 32px;border-top:1px solid #475569;margin-top:26px;padding-top:20px}.stats b{font-size:24px;display:block;color:#f8fafc}.stats span{font-size:12px;color:#cbd5e1;text-transform:uppercase;letter-spacing:.08em}
.intro,.chapter{background:var(--paper);border:1px solid var(--line);border-radius:12px;padding:28px 34px;margin-bottom:22px}.chapter{scroll-margin-top:24px}h2{font-size:27px;line-height:1.25;letter-spacing:-.025em;margin:6px 0 22px}h3{font-size:20px;line-height:1.35;margin:32px 0 14px}p{margin:0 0 18px}p:last-child{margin-bottom:0}strong{font-weight:700}code{font-size:.88em;background:var(--soft);padding:2px 5px;border-radius:4px;overflow-wrap:anywhere}pre{background:var(--soft);padding:20px;overflow:auto;border-radius:8px;line-height:1.6}pre code{background:none;padding:0}.table-scroll{overflow-x:auto;border:1px solid var(--line);border-radius:8px;margin:20px 0 24px}table{width:100%;border-collapse:collapse;min-width:560px;font-size:14px;line-height:1.6}th,td{padding:13px 15px;text-align:left;border-bottom:1px solid var(--line);vertical-align:top}th{background:var(--soft);font-weight:700}tr:last-child td{border-bottom:0}td a{overflow-wrap:anywhere}ul,ol{padding-left:24px}li{margin:8px 0}.rule:target{outline:3px solid var(--link);outline-offset:8px;border-radius:3px}.swatch{display:inline-block;width:16px;height:16px;border:1px solid var(--control);border-radius:4px;vertical-align:middle;margin-right:6px}.search-results{background:var(--paper);border:1px solid var(--line);border-radius:12px;padding:22px 28px;margin-bottom:24px}.search-results a{display:block;padding:8px 0;font-size:15px}#search-status{font-size:13px;color:var(--muted)}.foot{font-size:13px;color:var(--muted);padding:16px 5px}.mobile-note{font-size:13px;color:var(--muted)}
@media(max-width:1050px){.layout{grid-template-columns:240px minmax(0,1fr)}main{padding:28px 22px}.hero{padding:30px}.chapter,.intro{padding:25px}}
@media(max-width:760px){.layout{display:block}aside{position:static;height:auto;padding:18px 20px;border-right:0;border-bottom:1px solid var(--line)}.brand{margin-bottom:12px}.brand b{font-size:22px}main{padding:18px 12px 40px}.hero{border-radius:10px;padding:26px 23px}.hero h1{font-size:36px}.chapter,.intro{padding:23px 20px}h2{font-size:24px}.toc{max-height:50vh;overflow:auto}.nav-links{margin:8px 0}.stats{gap:20px}.table-scroll{margin-left:0;margin-right:0}p{overflow-wrap:break-word}}
@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto!important}}
@media print{:root,:root[data-theme=dark]{color-scheme:light;--bg:#fff;--paper:#fff;--text:#000;--muted:#333;--line:#aaa;--link:#000;--soft:#f5f5f5}body{font-size:10pt;line-height:1.5}.layout{display:block}aside,.skip,.search-results{display:none}main{padding:0;max-width:none}.hero{background:#fff;color:#000;border:1px solid #aaa;padding:25px;break-after:page}.hero p,.eyebrow,.stats span,.stats b{color:#000}.hero h1{font-size:36pt}.chapter,.intro{border:0;padding:0;margin:0 0 25px}.chapter h2{break-before:page;font-size:22pt}h2,h3{break-after:avoid}p,li{orphans:3;widows:3}table{min-width:0;font-size:9pt;table-layout:fixed}thead{display:table-header-group}tr{break-inside:avoid}th,td{padding:7px;overflow-wrap:anywhere}.table-scroll{overflow:visible;border:0}pre{white-space:pre-wrap}a{text-decoration:none}}
"""

JS = """
const picker=document.getElementById('theme');
const media=matchMedia('(prefers-color-scheme: dark)');
let preference='system';try{preference=localStorage.getItem('rulebook-theme')||'system'}catch{}
if(!['system','light','dark'].includes(preference))preference='system';
picker.value=preference;
function applyTheme(){document.documentElement.dataset.theme=preference==='system'?(media.matches?'dark':'light'):preference}
applyTheme();picker.addEventListener('change',()=>{preference=picker.value;try{localStorage.setItem('rulebook-theme',preference)}catch{}applyTheme()});media.addEventListener('change',applyTheme);
const nav=document.getElementById('contents');nav.open=matchMedia('(min-width: 761px)').matches;
const query=document.getElementById('search'),results=document.getElementById('search-results'),status=document.getElementById('search-status');
const index=Array.from(document.querySelectorAll('.rule')).map(e=>({id:e.id,text:e.textContent}));
query.addEventListener('input',()=>{const q=query.value.trim().toLowerCase();results.replaceChildren();results.hidden=!q;if(!q){status.textContent='';return}const terms=q.split(/\\s+/);const matches=index.filter(e=>terms.every(t=>e.text.toLowerCase().includes(t)));status.textContent=matches.length+' matching rules'+(matches.length>20?' (first 20 shown)':'');const heading=document.createElement('h2');heading.textContent='Matching rules';results.append(heading);if(!matches.length){const p=document.createElement('p');p.textContent='No matching rule. Try one word, a rule ID, or your browser’s Find command for tables and other content.';results.append(p)}for(const e of matches.slice(0,20)){const a=document.createElement('a');a.href='#'+e.id;a.textContent=e.text.length>200?e.text.slice(0,200)+'…':e.text;a.addEventListener('click',()=>{const el=document.getElementById(e.id);el.tabIndex=-1;el.focus({preventScroll:true})});results.append(a)}});
"""


def render(source, filename, title, subtitle, stats):
    md = markdown.Markdown(extensions=['tables', 'fenced_code', 'toc'], extension_configs={'toc': {'toc_depth': '2-2'}})
    soup = BeautifulSoup(md.convert(source), 'html.parser')
    first = soup.find('h1')
    if first:
        first.decompose()
    index_ids = []
    for element in soup.find_all(['p', 'tr']):
        match = re.match(r'([A-Z][A-Z0-9]+-\d{2})\b', element.get_text(' ', strip=True))
        if match:
            element['id'] = match[1].lower()
            element['class'] = 'rule'
            index_ids.append(match[1])
    if not index_ids:
        for i, item in enumerate(soup.find_all('li'), 1):
            item['id'] = f'check-{i}'
            item['class'] = 'rule'
    for table in soup.find_all('table'):
        previous = table.find_previous(['h2', 'h3'])
        wrapper = soup.new_tag('div', attrs={'class': 'table-scroll', 'tabindex': '0', 'role': 'region', 'aria-label': (previous.get_text() if previous else 'Reference') + ' table'})
        table.wrap(wrapper)
        for cell in table.select('thead th'):
            cell['scope'] = 'col'
    for code in soup.find_all('code'):
        if re.fullmatch(r'#[0-9A-Fa-f]{6}', code.get_text()):
            swatch = soup.new_tag('span', attrs={'class': 'swatch', 'aria-hidden': 'true', 'style': 'background:' + code.get_text()})
            code.insert_before(swatch)
    sections = []
    section = BeautifulSoup('<section class="intro"></section>', 'html.parser').section
    for item in list(soup.contents):
        if item.name == 'h2':
            if section.contents:
                sections.append(str(section))
            section = BeautifulSoup('<section class="chapter"></section>', 'html.parser').section
        section.append(item.extract())
    if section.contents:
        sections.append(str(section))
    prepaint = "try{let t=localStorage.getItem('rulebook-theme')||'system';document.documentElement.dataset.theme=t==='dark'||(t==='system'&&matchMedia('(prefers-color-scheme: dark)').matches)?'dark':'light'}catch{}"
    document = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><title>{escape(title)}</title><script>{prepaint}</script><style>{CSS}</style></head>
<body><a class="skip" href="#main">Skip to handbook</a><div class="layout">
<aside><div class="brand">Akash’s development standard<b>The Rulebook</b></div>
<label for="search">Find a rule</label><input type="search" id="search" placeholder="Password, sheet, SEC-01" aria-describedby="search-status"><div id="search-status" role="status" aria-live="polite"></div>
<label for="theme">Appearance</label><select id="theme"><option value="system">System</option><option value="light">Light</option><option value="dark">Dark</option></select>
<div class="nav-links"><a href="index.html">Rulebook</a><a href="checklist.html">Release checklist</a><a href="design-atlas.html">Design atlas</a></div>
<details id="contents" open><summary>Contents</summary><nav aria-label="Chapters">{md.toc}</nav></details>
<p class="mobile-note">Version 2.0 · 18 Sep 2026<br>Use browser Find for all text. Print to save a reading copy.</p></aside>
<main id="main" tabindex="-1"><header class="hero"><div class="eyebrow">Research → principles → practice</div><h1>{escape(title)}</h1><p>{escape(subtitle)}</p><div class="stats">{''.join('<div><b>'+escape(str(n))+'</b><span>'+escape(label)+'</span></div>' for n,label in stats)}</div></header>
<div id="search-results" class="search-results" hidden></div>{''.join(sections)}
<footer class="foot">Research-backed defaults, with context and evidence. No checklist replaces judgment or verification. <a href="RULEBOOK.md">Markdown source</a> · <a href="contrast-report.json">Palette verification</a></footer></main></div><script>{JS}</script></body></html>'''
    final = BeautifulSoup(document, 'html.parser')
    ids = [e['id'] for e in final.select('[id]')]
    assert len(ids) == len(set(ids)), 'Duplicate document anchors'
    broken = [a['href'] for a in final.select('a[href^="#"]') if a['href'][1:] not in ids]
    assert not broken, broken
    assert not re.search(r'\[S\d{2}\]', final.get_text()), 'Unresolved source reference'
    (ROOT / filename).write_text(document)
    return {'file': filename, 'rules': len(index_ids), 'chapters': len(final.select('main h2')), 'internal_links_valid': True, 'source_references_resolved': True}


def main():
    report = []
    schemes = {
        'light': {'surfaces': ['#FFFFFF', '#F8FAFC'], 'text': ['#0F172A', '#475569', '#1D4ED8', '#B91C1C', '#166534', '#92400E'], 'control': ['#64748B', '#1D4ED8'], 'button': ('#FFFFFF', '#1D4ED8')},
        'dark': {'surfaces': ['#0F172A', '#1E293B'], 'text': ['#F8FAFC', '#CBD5E1', '#93C5FD', '#FCA5A5', '#86EFAC', '#FDE68A'], 'control': ['#94A3B8', '#93C5FD'], 'button': ('#0F172A', '#93C5FD')},
    }
    for mode, values in schemes.items():
        pairs = [(f, b, 4.5, 'text') for f in values['text'] for b in values['surfaces']]
        pairs += [(f, b, 3, 'essential boundary or focus against neutral') for f in values['control'] for b in values['surfaces']]
        pairs += [(*values['button'], 4.5, 'button label')]
        for foreground, background, minimum, kind in pairs:
            ratio = contrast(foreground, background)
            report.append({'theme': mode, 'purpose': kind, 'foreground': foreground, 'background': background, 'ratio': round(ratio, 4), 'required': minimum, 'pass': ratio >= minimum})
    assert all(x['pass'] for x in report), [x for x in report if not x['pass']]
    (ROOT / 'contrast-report.json').write_text(json.dumps({'scope': 'Opaque sRGB starter palette pairs only; not a full interface accessibility audit.', 'formula': 'WCAG relative luminance; full precision used before comparison', 'pairs': report}, indent=2) + '\n')
    source = (ROOT / 'RULEBOOK.md').read_text()
    rules = re.findall(r'(?:\*\*|\| )([A-Z][A-Z0-9]+-\d{2})\b', source)
    assert len(rules) == len(set(rules))
    references = re.findall(r'^\[S\d+\]: (https://\S+)', source, re.M)
    outputs = [render(source, 'index.html', 'The Development Rulebook', 'A practical standard for building products that people can understand, use, trust, and return to.', [(len(rules), 'numbered rules'), (len(re.findall(r'^## \d{2} ·', source, re.M)), 'chapters'), (len(references), 'sources')])]
    outputs += [render((ROOT / 'RELEASE-CHECKLIST.md').read_text(), 'checklist.html', 'Acceptance & release checklist', 'Select the checks that apply. Record evidence, owners, exceptions, and unresolved gaps before calling a release ready.', [('Pass / Fail', 'actual evidence'), ('N/A', 'with a reason'), ('No guesses', 'about readiness')])]
    outputs += [render((ROOT / 'DESIGN-ATLAS.md').read_text(), 'design-guide.html', 'Design & Interaction Guide', 'Choose a coherent direction, define behavior, and verify the enhancement in context.', [(120, 'pattern recipes'), (18, 'art directions'), (34, 'source records')])]
    verification = {'markdown_sha256': hashlib.sha256(source.encode()).hexdigest(), 'unique_rule_ids': len(rules), 'sources': len(references), 'contrast_pairs_passed': len(report), 'html': outputs, 'limitations': ['Computed palette checks are not a full accessibility audit.', 'Source references are resolved locally; this does not guarantee future URL availability.', 'No existing application has been audited by creating this handbook.']}
    (ROOT / 'verification.json').write_text(json.dumps(verification, indent=2) + '\n')
    print(json.dumps(verification, indent=2))


if __name__ == '__main__':
    main()
