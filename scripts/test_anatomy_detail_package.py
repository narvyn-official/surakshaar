"""Negative authoring checks for the optional quality archive, independent of Android installation."""
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

spec=importlib.util.spec_from_file_location('detail_package',Path(__file__).parent/'anatomy/package_detail.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)


class DetailPackageTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.base=self.root/'base/anatomy';self.detail=self.root/'detail/anatomy';self.output=self.base/'optional'
        vertices=[-1.,-1.,0.,0.,0.,1., 1.,-1.,0.,0.,0.,1., 0.,1.,0.,0.,0.,1.]
        payload=gzip.compress(b'ARA1'+struct.pack('<I18f',3,*vertices),mtime=0)
        part={'id':'source-test','name':'Test geometry','layer':'skeleton','file':'anatomy/source-test.meshpack',
              'bounds':[-1.,-1.,0.,1.,1.,0.],'vertices':3,'colour':[.8,.7,.6],
              'explanation':'Authoring fixture only','sourceElements':['original-source'],'bytes':len(payload),'sha256':hashlib.sha256(payload).hexdigest()}
        self.manifest={'version':'detail-test','quality':'detail','licence':'CC-BY-4.0','licenceUrl':'https://example.com/licence','attribution':'Fixture source','parts':[part],
                       'units':'metres','axes':'X anatomical left, Y superior, Z anterior','heightMetres':1.72}
        for folder in (self.base,self.detail):
            folder.mkdir(parents=True)
            (folder/'source-test.meshpack').write_bytes(payload)
            (folder/'LICENSE.txt').write_text('Fixture source\nhttps://example.com/licence')
            (folder/'LICENSE-LUNGS.txt').write_text('No external lungs in this isolated fixture')
            (folder/'manifest.json').write_text(json.dumps(self.manifest))

    def save(self):
        (self.detail/'manifest.json').write_text(json.dumps(self.manifest))

    def test_archive_is_reproducible_and_catalogue_describes_exact_bytes(self):
        first=module.package(self.base,self.detail,self.output)
        second=module.package(self.base,self.detail,self.root/'second')
        self.assertEqual(first,second)
        blob=(self.output/'body-detail.zip').read_bytes()
        self.assertEqual(first['sha256'],hashlib.sha256(blob).hexdigest())
        with zipfile.ZipFile(self.output/'body-detail.zip') as archive:
            self.assertEqual({'manifest.json','anatomy/source-test.meshpack','LICENSE.txt','LICENSE-LUNGS.txt'},set(archive.namelist()))
            self.assertEqual(first['unpackedBytes'],sum(row.file_size for row in archive.infolist()))

    def test_quality_cannot_swap_semantic_identity(self):
        self.manifest['parts'][0]['id']='other-id';self.save()
        with self.assertRaisesRegex(AssertionError,'identical semantic IDs'):
            module.package(self.base,self.detail,self.output)

    def test_quality_cannot_relabel_a_structure(self):
        self.manifest['parts'][0]['name']='Unrelated anatomy';self.save()
        with self.assertRaisesRegex(AssertionError,'changed semantics'):
            module.package(self.base,self.detail,self.output)

    def test_corrupted_source_mesh_cannot_be_archived(self):
        (self.detail/'source-test.meshpack').write_bytes(b'corrupted')
        with self.assertRaisesRegex(ValueError,'byte count mismatch'):
            module.package(self.base,self.detail,self.output)

    def test_wrong_quality_is_rejected(self):
        self.manifest['quality']='balanced';self.save()
        with self.assertRaisesRegex(AssertionError,'detailed-quality'):
            module.package(self.base,self.detail,self.output)

    def test_release_validator_checks_optional_archive(self):
        module.package(self.base,self.detail,self.output)
        result=module.validator.validate_optional_pack(self.base.parent,self.manifest)
        self.assertEqual(1,result['triangles'])
        self.assertEqual({'CC-BY-4.0':1},result['licencesByPart'])

    def test_release_validator_rejects_archive_corruption(self):
        module.package(self.base,self.detail,self.output)
        path=self.output/'body-detail.zip';blob=path.read_bytes();path.write_bytes(blob[:-1]+bytes([blob[-1]^1]))
        with self.assertRaisesRegex(ValueError,'archive SHA-256 mismatch'):
            module.validator.validate_optional_pack(self.base.parent,self.manifest)

    def test_release_validator_rejects_another_base_version(self):
        module.package(self.base,self.detail,self.output)
        other=dict(self.manifest,version='other-version')
        with self.assertRaisesRegex(ValueError,'another base version'):
            module.validator.validate_optional_pack(self.base.parent,other)

    def test_release_validator_does_not_ignore_unlisted_asset(self):
        module.package(self.base,self.detail,self.output)
        (self.output/'unexpected.meshpack').write_bytes(b'not-in-the-catalogue')
        with self.assertRaisesRegex(ValueError,'unexpected'):
            module.validator.validate_optional_pack(self.base.parent,self.manifest)


if __name__=='__main__':unittest.main()
