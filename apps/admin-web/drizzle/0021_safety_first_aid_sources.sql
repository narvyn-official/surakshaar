-- Proposals only. Existing administrator decisions and curriculum remain unchanged.
INSERT OR IGNORE INTO safety_sources(id,title,publisher,url,topic,jurisdiction,rationale,changed_at) VALUES
 ('nhs-burns','Burns and scalds','NHS','https://www.nhs.uk/conditions/burns-and-scalds/','first-aid','United Kingdom','Cooling, stuck clothing and loose covering references for burn/scald scene review. UK emergency numbers require local adaptation; qualified clinical review remains separate.',0),
 ('nhs-cuts','Cuts and grazes','NHS','https://www.nhs.uk/conditions/cuts-and-grazes/','first-aid','United Kingdom','Wound protection, direct-pressure and embedded-object exceptions for staff reference. Distinguish minor cuts from severe bleeding; UK emergency pathways require local adaptation.',0);
