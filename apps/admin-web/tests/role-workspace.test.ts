import test from 'node:test';
import assert from 'node:assert/strict';
import {roles,permitted} from '../lib/access';
import {workspaceDefinitions,roleAllowsPage,legacyTabs} from '../lib/role-workspace';

test('operational workspaces cannot land in learning or borrow another role through navigation',()=>{
 for(const role of ['platform',...roles,'supporter'] as const){assert.equal(roleAllowsPage(role,'/studio'),false,role);assert.equal(roleAllowsPage(role,'/identity'),false,role);}
 assert.equal(roleAllowsPage('platform','/platform'),true);
 for(const role of roles)assert.equal(roleAllowsPage(role,'/platform'),false);
 assert.deepEqual(legacyTabs('auditor'),[]);
 for(const role of ['personal','learner'] as const)assert.equal(roleAllowsPage(role,'/studio'),true);
});
test('auditor is restricted to metadata; review, authorship and issuance stay distinct',()=>{
 for(const p of ['read','write','admin','certify','revoke','review','identity','content','content-review'] as const)assert.equal(permitted('auditor',p),false,p);
 assert.equal(permitted('auditor','audit'),true);
 assert.equal(permitted('examiner','certify'),false);
 assert.equal(permitted('certifier','identity'),false);
 assert.equal(permitted('author','content-review'),false);
 assert.equal(permitted('assessor','identity')&&permitted('assessor','certify'),true);
 assert.equal(workspaceDefinitions.reviewer.actions.length,1);
});
