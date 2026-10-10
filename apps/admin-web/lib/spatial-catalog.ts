import source from '../../../content/spatial/catalog.json';
export type SpatialPart={id:string;name:string;point:number[];distance:number;yaw?:number;elevation?:number;description:string};
export type SpatialPlan={model:string;part:string;kind:'equipment'|'anatomy'|'teaching';teachingKey?:string;observationPart?:string;audience:string;title:string;boundary:string};
export const spatialModels=source.models as Record<string,{id:string;name:string;parts:SpatialPart[]}>;
export const spatialSteps=source.steps as Record<string,SpatialPlan>;
export function modelFamily(id:string){return id==='fire'||['fire-damaged','fire-pin-removed','fire-aimed','fire-discharging'].includes(id)?'fire':id.startsWith('machinery')?'machinery':id.startsWith('ppe')?'ppe':id;}
export function partsForModel(id:string){return spatialModels[modelFamily(id)]?.parts??[];}
export function planForStep(scenario:string,step:string){return spatialSteps[scenario+'/'+step];}
export const modelVariants:Record<string,{id:string;name:string}[]>={
 fire:[{id:'fire',name:'Intact unit'},{id:'fire-damaged',name:'Damaged hose'},{id:'fire-pin-removed',name:'Pin removed'},{id:'fire-aimed',name:'Hose directed'},{id:'fire-discharging',name:'Discharge illustration'}],
 machinery:[{id:'machinery',name:'Guard & lock fitted'},{id:'machinery-unlocked',name:'Lock absent'},{id:'machinery-guard-missing',name:'Guard absent'}],
 ppe:[{id:'ppe',name:'Intact equipment'},{id:'ppe-damaged',name:'Damage specimen'}],
};
