package org.segrada.service.repository.orientdb.base;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.segrada.model.base.AbstractAnnotatedModel;
import org.segrada.model.prototype.IComment;
import org.segrada.model.prototype.IFile;
import org.segrada.model.prototype.ISourceReference;
import org.segrada.model.prototype.SegradaAnnotatedEntity;
import org.segrada.service.repository.orientdb.factory.OrientDbRepositoryFactory;
import org.segrada.session.Identity;
import org.segrada.test.OrientDBTestInstance;
import org.segrada.test.OrientDbTestApplicationSettings;

import java.util.List;

import static org.junit.Assert.*;

public class AbstractAnnotatedOrientDbRepositoryTest {
	/**
	 * reference to test instance of orientdb in memory
	 */
	private OrientDBTestInstance orientDBTestInstance = new OrientDBTestInstance();

	/**
	 * reference to factory
	 */
	private OrientDbRepositoryFactory factory;

	/**
	 * mock repository - see below
	 */
	private MockOrientDbRepository mockOrientDbRepository;

	@Before
	public void setUp() throws Exception {
		// set up schema if needed
		orientDBTestInstance.setUpSchemaIfNeeded();

		// open database
		ODatabaseDocumentTx db = orientDBTestInstance.getDatabase();

		// create schema
		db.command(new OCommandSQL("create class Mock extends V")).execute();

		factory = new OrientDbRepositoryFactory(db, new OrientDbTestApplicationSettings(), new Identity());

		// create repo
		mockOrientDbRepository = new MockOrientDbRepository(factory);
		// add repository
		factory.addRepository(mockOrientDbRepository.getClass(), mockOrientDbRepository);
	}

	@After
	public void tearDown() throws Exception {
		// close db
		try {
			mockOrientDbRepository.db.close();
		} catch (Exception e) {
			// do nothing
		}

		// open database
		ODatabaseDocumentTx db = orientDBTestInstance.getDatabase();

		// remove schema
		db.command(new OCommandSQL("delete vertex V")).execute();
		db.command(new OCommandSQL("delete edge E")).execute();
		db.command(new OCommandSQL("drop class Mock")).execute();

		// close db
		db.close();
	}

	@Test
	public void testPopulateODocumentWithAnnotated() throws Exception {
		MockEntity mockEntity = new MockEntity();

		ODocument document = mockOrientDbRepository.convertToDocument(mockEntity);

		// does nothing, will not change anything, but will also not throw an exception
		mockOrientDbRepository.populateODocumentWithAnnotated(document, mockEntity);
	}

	@Test
	public void testPopulateEntityWithAnnotated() throws Exception {
		// create mock entity
		ODocument document = new ODocument("Mock").field("created", 1L).field("modified", 2L);
		document.save();

		// create tag
		ODocument tag = new ODocument("Tag").field("title", "title").field("created", 1L).field("modified", 2L);
		tag.save();
		// ... and connect
		factory.getDb().command(new OCommandSQL("create edge IsTagOf from " + tag.getIdentity().toString() + " to " + document.getIdentity().toString())).execute();

		// create source
		ODocument source = new ODocument("Source").field("shortTitle", "shortTitle").field("shortRef", "shortRef")
				.field("created", 1L).field("modified", 2L);
		source.save();

		// create source reference
		ODocument sourceReference = new ODocument("SourceReference").field("source", source).field("reference", document)
				.field("created", 1L).field("modified", 2L);
		sourceReference.save();

		// create comment
		ODocument comment = new ODocument("Comment").field("text", "text").field("markup", "markup")
				.field("created", 1L).field("modified", 2L);
		comment.save();
		// ... and connect
		factory.getDb().command(new OCommandSQL("create edge IsCommentOf from " + comment.getIdentity().toString() + " to " + document.getIdentity().toString())).execute();

		// create a file
		ODocument file = new ODocument("File").field("filename", "filename.txt").field("mimeType", "mimeType")
				.field("indexFullText", true).field("containFile", false).field("fileIdentifier", "fileIdentifier.txt")
				.field("descriptionMarkup", "default").field("created", 1L).field("modified", 2L);
		file.save();
		// ... and connect
		factory.getDb().command(new OCommandSQL("create edge IsFileOf from " + file.getIdentity().toString() + " to " + document.getIdentity().toString())).execute();

		// do conversion
		MockEntity mockEntity = mockOrientDbRepository.convertToEntity(document);
		// populate!
		mockOrientDbRepository.populateEntityWithAnnotated(document, mockEntity);

		// get tags
		String[] tags = mockEntity.getTags();
		assertEquals(1, tags.length);
		assertEquals("title", tags[0]);

		// get source references
		List<ISourceReference> list = mockEntity.getSourceReferences();
		assertFalse(list.isEmpty());
		assertEquals(mockEntity.getId(), list.get(0).getReference().getId());
		assertEquals(source.getIdentity().toString(), list.get(0).getSource().getId());

		List<IComment> list1 = mockEntity.getComments();
		assertFalse(list1.isEmpty());
		assertEquals(comment.getIdentity().toString(), list1.get(0).getId());

		List<IFile> list2 = mockEntity.getFiles();
		assertFalse(list2.isEmpty());
		assertEquals(file.getIdentity().toString(), list2.get(0).getId());
	}

	@Test
	public void testLazyLoadSourceReferences() throws Exception {
		// create mock entity
		MockEntity mockEntity = new MockEntity();
		mockOrientDbRepository.save(mockEntity);

		List<ISourceReference> list = mockOrientDbRepository.lazyLoadSourceReferences(mockEntity);
		assertTrue(list.isEmpty());

		// create source
		ODocument source = new ODocument("Source").field("shortTitle", "shortTitle").field("shortRef", "shortRef")
				.field("created", 1L).field("modified", 2L);
		source.save();

		// create source reference
		ODocument sourceReference = new ODocument("SourceReference").field("source", source).field("reference", new ORecordId(mockEntity.getId()))
				.field("created", 1L).field("modified", 2L);
		sourceReference.save();

		// load again with created references
		list = mockOrientDbRepository.lazyLoadSourceReferences(mockEntity);
		assertFalse(list.isEmpty());
		assertEquals(mockEntity.getId(), list.get(0).getReference().getId());
		assertEquals(source.getIdentity().toString(), list.get(0).getSource().getId());
	}

	@Test
	public void testLazyLoadComments() throws Exception {
		// create mock entity
		MockEntity mockEntity = new MockEntity();
		mockOrientDbRepository.save(mockEntity);

		List<IComment> list = mockOrientDbRepository.lazyLoadComments(mockEntity);
		assertTrue(list.isEmpty());

		// create comment
		ODocument comment = new ODocument("Comment").field("text", "text").field("markup", "markup")
				.field("created", 1L).field("modified", 2L);
		comment.save();
		// ... and connect
		factory.getDb().command(new OCommandSQL("create edge IsCommentOf from " + comment.getIdentity().toString() + " to " + mockEntity.getId())).execute();

		// load again with created references
		list = mockOrientDbRepository.lazyLoadComments(mockEntity);
		assertFalse(list.isEmpty());
		assertEquals(comment.getIdentity().toString(), list.get(0).getId());
	}

	@Test
	public void testLazyLoadFiles() throws Exception {
		// create mock entity
		MockEntity mockEntity = new MockEntity();
		mockOrientDbRepository.save(mockEntity);

		List<IFile> list = mockOrientDbRepository.lazyLoadFiles(mockEntity);
		assertTrue(list.isEmpty());

		// create a file
		ODocument file = new ODocument("File").field("filename", "filename.txt").field("mimeType", "mimeType")
				.field("indexFullText", true).field("containFile", false).field("fileIdentifier", "fileIdentifier.txt")
				.field("descriptionMarkup", "default").field("created", 1L).field("modified", 2L);
		file.save();
		// ... and connect
		factory.getDb().command(new OCommandSQL("create edge IsFileOf from " + file.getIdentity().toString() + " to " + mockEntity.getId())).execute();

		// load again with created references
		list = mockOrientDbRepository.lazyLoadFiles(mockEntity);
		assertFalse(list.isEmpty());
		assertEquals(file.getIdentity().toString(), list.get(0).getId());
	}

	@Test
	public void testProcessAfterSaving() throws Exception {
		fail("Implement test");

	}

	@Test
	public void testUpdateEntityTags() throws Exception {
		fail("Implement test");

	}

	@Test
	public void testDelete() throws Exception {
		fail("Implement method and test deletion");
	}

	/**
	 * Mock entity
	 */
	private class MockEntity extends AbstractAnnotatedModel implements SegradaAnnotatedEntity {
		private String id;

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getTitle() {
			return "DUMMY";
		}
	}

	/**
	 * Partial/mock repository to test methods
	 */
	private class MockOrientDbRepository extends AbstractAnnotatedOrientDbRepository<MockEntity> {
		public MockOrientDbRepository(OrientDbRepositoryFactory repositoryFactory) {
			super(repositoryFactory);
		}

		@Override
		public MockEntity convertToEntity(ODocument document) {
			MockEntity entity = new MockEntity();
			populateEntityWithBaseData(document, entity);
			return entity;
		}

		@Override
		public ODocument convertToDocument(MockEntity entity) {
			return createOrLoadDocument(entity);
		}

		@Override
		public String getModelClassName() {
			return "Mock";
		}
	}
}