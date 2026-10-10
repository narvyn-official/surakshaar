# Anatomy selection descriptions

Updated 10 October 2026. `AnatomyKnowledge.kt` contains concise, original descriptions of the labelled structures in the shipped BodyParts3D packs. These are educational anatomy facts, not diagnoses, treatment instructions, clinical approval or a claim that the geometry has received professional review.

## Selection contract

- The lookup uses the **exact mesh ID**, never its display name, current scenario, nearest lesson target or list position.
- All **117 IDs** in both balanced and detailed manifests have descriptions, locations, functions and reference URLs.
- English and Hindi descriptions are included. Unsupported languages return English with `language = "en"`; the interface must identify the fallback. Hindi terminology and narration remain subject to qualified language review.
- Left and right are the person's anatomical sides, not the viewer's screen sides.
- The skeletal remainder and seven muscle remainder meshes explicitly describe **groups**. They cannot identify each muscle or bone that the source geometry combines. Rib cage and vertebral column are also identified as multi-structure selections.
- Separate portions of deltoid, trapezius, pectoralis, biceps, triceps, gastrocnemius and quadriceps retain their anatomical distinctions. In particular, the short head of biceps femoris is not described as a hip extensor.
- `info()` returns `null` for unknown IDs. The caller must show an honest unavailable description rather than reuse the previous selection, a first-aid scenario or a guessed structure.
- `introText` is derived from the same selected entry that supplies the visible description. It does not add first-aid procedures or unrelated safety advice.
- No 3D meshes, pack manifests, assessment answers, curriculum versions or clinical-review states are changed by this catalog.

## Factual references

The references below were checked through current official publisher or institution pages on 10 October 2026. Some NCBI pages served a browser challenge on direct opening; their publisher-indexed text was available through search. This is a factual reference check, not clinical sign-off. No source illustrations, tables, exercises or passages are reproduced in the app.

| Structures | References |
| --- | --- |
| Skin | [OpenStax: Layers of the skin](https://openstax.org/books/anatomy-and-physiology-2e/pages/5-1-layers-of-the-skin) |
| Sternum and rib cage | [OpenStax: Thoracic cage](https://openstax.org/books/anatomy-and-physiology-2e/pages/7-4-the-thoracic-cage) |
| Vertebral column | [OpenStax: Vertebral column](https://openstax.org/books/anatomy-and-physiology-2e/pages/7-3-the-vertebral-column) |
| Skeletal remainder | [OpenStax: Functions of the skeletal system](https://openstax.org/books/anatomy-and-physiology-2e/pages/6-1-the-functions-of-the-skeletal-system) |
| Humerus and radius | [OpenStax: Bones of the upper limb](https://openstax.org/books/anatomy-and-physiology-2e/pages/8-2-bones-of-the-upper-limb) |
| Femur and tibia | [OpenStax: Bones of the lower limb](https://openstax.org/books/anatomy-and-physiology-2e/pages/8-4-bones-of-the-lower-limb) |
| Trachea and lungs | [NHLBI: Respiratory system](https://www.nhlbi.nih.gov/health/lungs/respiratory-system) |
| Heart | [NHLBI: Heart anatomy](https://www.nhlbi.nih.gov/health/heart/anatomy) |
| Aorta | [NHLBI: Blood flow through the heart](https://www.nhlbi.nih.gov/health/heart/blood-flow) |
| Diaphragm | [NHLBI: How the body controls breathing](https://www.nhlbi.nih.gov/health/lungs/body-controls-breathing) |
| Brain and spinal cord | [OpenStax: Central nervous system](https://openstax.org/books/anatomy-and-physiology-2e/pages/13-2-the-central-nervous-system) |
| Liver | [NIDDK: Digestive system](https://www.niddk.nih.gov/health-information/digestive-diseases/digestive-system-how-it-works) |
| Kidneys | [NIDDK: Kidneys and how they work](https://www.niddk.nih.gov/health-information/kidney-disease/kidneys-how-they-work) |
| Trapezius portions | [StatPearls on NCBI: Trapezius](https://www.ncbi.nlm.nih.gov/books/NBK518994/) |
| Deltoid portions | [StatPearls on NCBI: Deltoid](https://www.ncbi.nlm.nih.gov/books/NBK537056/) |
| Pectoralis major and minor | [StatPearls on NCBI: Pectoral muscles](https://www.ncbi.nlm.nih.gov/books/NBK545241/), [OpenStax: Pectoral girdle and upper limbs](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-5-muscles-of-the-pectoral-girdle-and-upper-limbs) |
| Serratus anterior and supraspinatus | [StatPearls on NCBI: Arm abductor muscles](https://www.ncbi.nlm.nih.gov/books/NBK537148/) |
| Biceps brachii | [StatPearls on NCBI: Shoulder and upper-limb muscles](https://www.ncbi.nlm.nih.gov/books/NBK482410/) |
| Triceps heads | [StatPearls on NCBI: Triceps](https://www.ncbi.nlm.nih.gov/books/NBK536996/) |
| Brachialis and coracobrachialis | [StatPearls on NCBI: Arm structure and function](https://www.ncbi.nlm.nih.gov/books/NBK507841/) |
| Infraspinatus | [StatPearls on NCBI: Infraspinatus](https://www.ncbi.nlm.nih.gov/books/NBK513255/) |
| Teres major | [StatPearls on NCBI: Scapulohumeral muscles](https://www.ncbi.nlm.nih.gov/books/NBK546633/) |
| Teres minor | [StatPearls on NCBI: Rotator cuff](https://www.ncbi.nlm.nih.gov/books/NBK441844/) |
| Brachioradialis | [StatPearls on NCBI: Brachioradialis](https://www.ncbi.nlm.nih.gov/books/NBK526110/) |
| Upper-limb groups | [OpenStax: Pectoral girdle and upper limbs](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-5-muscles-of-the-pectoral-girdle-and-upper-limbs) |
| Sternocleidomastoid and head/neck/back groups | [OpenStax: Head, neck and back muscles](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-3-axial-muscles-of-the-head-neck-and-back) |
| External oblique and chest group | [OpenStax: Abdominal wall and thorax](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-4-axial-muscles-of-the-abdominal-wall-and-thorax) |
| Gluteus maximus | [StatPearls on NCBI: Gluteus maximus](https://www.ncbi.nlm.nih.gov/books/NBK538193/) |
| Gluteus medius | [StatPearls on NCBI: Gluteus medius](https://www.ncbi.nlm.nih.gov/books/NBK557509/) |
| Gluteus minimus | [StatPearls on NCBI: Gluteus minimus](https://www.ncbi.nlm.nih.gov/books/NBK556144/) |
| Sartorius | [StatPearls on NCBI: Sartorius](https://www.ncbi.nlm.nih.gov/books/NBK532889/) |
| Rectus femoris and vastus muscles | [StatPearls on NCBI: Femoral muscles](https://www.ncbi.nlm.nih.gov/books/NBK500008/) |
| Semitendinosus and semimembranosus | [StatPearls on NCBI: Hamstrings](https://www.ncbi.nlm.nih.gov/books/NBK546688/) |
| Biceps femoris heads | [StatPearls on NCBI: Posterior thigh](https://www.ncbi.nlm.nih.gov/books/NBK554598/) |
| Adductor longus and magnus | [StatPearls on NCBI: Medial thigh muscles](https://www.ncbi.nlm.nih.gov/books/NBK534775/) |
| Tibialis anterior | [StatPearls on NCBI: Foot muscles](https://www.ncbi.nlm.nih.gov/books/NBK539705/) |
| Soleus | [StatPearls on NCBI: Calf](https://www.ncbi.nlm.nih.gov/books/NBK459362/) |
| Gastrocnemius heads | [StatPearls on NCBI: Gastrocnemius](https://www.ncbi.nlm.nih.gov/books/NBK532946/) |
| Lower-limb and pelvic groups | [OpenStax: Pelvic girdle and lower limbs](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-6-appendicular-muscles-of-the-pelvic-girdle-and-lower-limbs) |
| Model identifiers and grouped geometry | [BodyParts3D source archive](https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html), shipped `content/anatomy/manifest.json` and optional `body-detail.zip` manifest |

The app text is an original concise explanation of basic facts. Source text, artwork and model geometry have separate rights; adding a factual reference does not import a publisher's book, illustrations or brand. The source model's existing attribution and licence files remain in place.

## Acceptance checks

`AnatomyKnowledgeTest` checks coverage against both **actual bundled manifests**, exact-ID fallback, English/Hindi availability, description length, source URL format, correct laterality, matched narration, honest grouping and easily confused structures. App rendering, mesh picking and narration behaviour require the separate activity and device checks; these unit tests do not establish anatomical geometry correctness or clinical approval.
