package cz.znj.kvr.sw.exp.java.mapdb.names;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class MapdbNamesTest
{
	@Test
	public void			testNames() throws IOException
	{
		File dbDir = Files.createTempDirectory("mapdb-exp").toFile();
		try {
			dbDir.deleteOnExit();
			File dbFile = new File(dbDir, "dbfile");

			DB db = DBMaker
					.newFileDB(dbFile)
					.make();
			try {
				db.createAtomicBoolean("hello", true);
				db.getAtomicBoolean("double").set(true);
				db.getAtomicBoolean("double").set(true);
				db.namedPut("named", true);

				Assert.assertTrue(db.exists("hello"));
				Assert.assertTrue(db.getAtomicBoolean("hello").get());
				Assert.assertTrue(db.exists("double"));
				Assert.assertTrue(db.getAtomicBoolean("double").get());
				Assert.assertFalse(db.exists("named"));

				db.commit();
			}
			finally {
				db.close();
			}

			DB db2 = DBMaker
					.newFileDB(dbFile)
					.make();
			try {
				Assert.assertTrue(db2.exists("hello"));
				Assert.assertTrue(db2.getAtomicBoolean("hello").get());
				Assert.assertTrue(db2.exists("double"));
				Assert.assertTrue(db2.getAtomicBoolean("double").get());
				Assert.assertFalse(db2.exists("named"));
				Assert.assertFalse(db2.exists("none"));
			}
			finally {
				db2.close();
			}
		}
		finally {
			FileUtils.deleteDirectory(dbDir);
		}
	}
}
