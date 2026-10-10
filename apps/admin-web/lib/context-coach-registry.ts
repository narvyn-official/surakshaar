import learning from '../../../content/learning/scenarios.json';
import type {CoachCard, CoachRegistry} from './context-coach';

const controls: CoachCard[] = [
  {id: 'controls-return-en', intent: 'controls', language: 'en', sourceId: 'learning-controls', text: 'Return to the current lesson to review its cue. Inspection changes what you see; it does not complete the decision or change your result.', actionIds: ['show-step']},
  {id: 'controls-selection-en', intent: 'controls', language: 'en', sourceId: 'learning-controls', text: 'Inspect the selected item, then return to the same lesson step. A selection is an exploration action, not evidence of practical skill.', actionIds: ['inspect-selection', 'show-step'], requiresSelection: true},
  {id: 'controls-sources-en', intent: 'controls', language: 'en', sourceId: 'learning-controls', text: 'Check the lesson sources and review status. A linked guideline does not mean this draft lesson has received qualified approval.', actionIds: ['open-sources']},
  {id: 'controls-return-hi', intent: 'controls', language: 'hi', sourceId: 'learning-controls', text: 'संकेत फिर से पढ़ने के लिए वर्तमान पाठ पर लौटें। मॉडल देखने से निर्णय पूरा नहीं होता और परिणाम नहीं बदलता।', actionIds: ['show-step']},
  {id: 'controls-selection-hi', intent: 'controls', language: 'hi', sourceId: 'learning-controls', text: 'चयनित भाग देखें, फिर पाठ के उसी चरण पर लौटें। किसी भाग का चयन करना व्यावहारिक कौशल का प्रमाण नहीं है।', actionIds: ['inspect-selection', 'show-step'], requiresSelection: true},
  {id: 'controls-sources-hi', intent: 'controls', language: 'hi', sourceId: 'learning-controls', text: 'पाठ के स्रोत और समीक्षा की स्थिति देखें। दिशानिर्देश का लिंक होने का अर्थ यह नहीं है कि इस मसौदे को योग्य विशेषज्ञ की मंज़ूरी मिली है।', actionIds: ['open-sources']},
];

/** Explicitly contains no draft medical/fire answers or correct-choice flags. */
export const contextCoachRegistry: CoachRegistry = {
  version: learning.version,
  sources: [{id: 'learning-controls', version: learning.version, title: 'SurakshaAR contextual learning control contract', status: 'implementation-reference'}],
  scenarios: learning.scenarios.map(scenario => {
    const courseId = scenario.course;
    if (courseId !== 'fire' && courseId !== 'first-aid') throw new Error('Unsupported contextual coaching course.');
    return {
    id: scenario.id,
    courseId,
    reviewStatus: scenario.reviewStatus,
    reviewer: scenario.reviewer,
    steps: scenario.steps.map(step => ({
      id: step.id,
      objectIds: step.part ? [step.part] : [],
      actionIds: step.choices.map(choice => choice.id),
      cards: controls,
    })),
    };
  }),
};
