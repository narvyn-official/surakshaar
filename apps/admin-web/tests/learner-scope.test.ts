import {test} from 'node:test';
import assert from 'node:assert/strict';
import {selectLearnerLink,centreId} from '../lib/learner-scope';
const a={owner:'centre-a',worker_id:'worker',name:'Learner',sector:'Unspecified',centre:'First centre'},b={...a,owner:'centre-b',centre:'Second centre'};
test('single centre upgrades keep working without a supplied owner',()=>assert.deepEqual(selectLearnerLink([a],'worker'),a));
test('several institutions never resolve through row order',()=>{for(const links of [[a,b],[b,a]])assert.throws(()=>selectLearnerLink(links,'worker'),/choose a training centre/)});
test('owner selection is constrained to the current account and worker links',()=>{assert.deepEqual(selectLearnerLink([a,b],'worker','centre-b'),b);assert.throws(()=>selectLearnerLink([a,b],'other','centre-a'),/Forbidden/);assert.throws(()=>selectLearnerLink([a,b],'worker','foreign'),/Forbidden/);assert.throws(()=>selectLearnerLink([a,a],'worker','centre-a'),/choose/)});
test('centre identifiers cannot inject paths, state namespaces or unbounded values',()=>{for(const value of ['','owner:review:other','a/b','x'.repeat(129),' spaced '])assert.equal(centreId.safeParse(value).success,false)});
