import source from '../public/models/spatial/procedures.json';
export const spatialProcedures=source.modules;
export type RehearsalEvent={kind:'choose';id:string}|{kind:'continue'};
export type RehearsalState={index:number;correct:boolean;mistakes:number;completed:string[];finished:boolean};
export function replayRehearsal(moduleId:string,events:RehearsalEvent[]):RehearsalState{
 const procedure=spatialProcedures.find(m=>m.id===moduleId);if(!procedure||events.length>2000)throw Error('Invalid rehearsal');
 const state:RehearsalState={index:0,correct:false,mistakes:0,completed:[],finished:false};
 for(const event of events){if(state.finished)throw Error('Rehearsal already finished');const step=procedure.steps[state.index];if(event.kind==='choose'){
   if(state.correct)throw Error('Continue to the next step');const action=step.actions.find(a=>a.id===event.id);if(!action)throw Error('Unknown scene action');if(action.correct){state.correct=true;state.completed.push(action.id);}else state.mistakes++;
  }else if(event.kind==='continue'){if(!state.correct)throw Error('Review this step before continuing');state.index++;state.correct=false;state.finished=state.index===procedure.steps.length;}else throw Error('Invalid event');}
 return state;
}
export function rehearsalModel(module:string,step:string,actions:string[]):string{
 const completed=new Set(actions);
 if(module==='fire'){
  if(step==='fire-alarm')return 'fire-alarm-stand';if(['fire-withdraw','fire-exit'].includes(step))return 'fire-exit';if(step==='fire-assembly')return 'fire-rollcall';if(step==='fire-handover')return 'fire-handover';
  if(completed.has('fire-squeeze')&&!completed.has('fire-withdraw'))return 'fire-discharging';if(completed.has('fire-aim'))return 'fire-aimed';if(completed.has('fire-pin'))return 'fire-pin-removed';return 'fire';
 }
 if(module==='machinery')return completed.has('mach-shift')?'machinery-guard-missing':completed.has('mach-lock')?'machinery':'machinery-unlocked';
 if(module==='ppe')return completed.has('ppe-crack')&&completed.has('ppe-eye')?'ppe':'ppe-damaged';
 return module;
}
export function parseRehearsal(raw:string|null,module:string):RehearsalEvent[]{if(!raw)return [];try{const data=JSON.parse(raw);if(data.version!==source.version||data.module!==module||!Array.isArray(data.events))return [];replayRehearsal(module,data.events);return data.events;}catch{return [];}}
export const rehearsalVersion=source.version;
