package lucene.sw;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndexHandler {
	
	private String indexPath;
	private String docPath;
	private boolean debug;
	
	public IndexHandler(String indexPath, String docPath, boolean debug) throws Exception {
		this.indexPath = indexPath;
		this.docPath = docPath;
		this.debug = debug;
		
		boolean createIndex = false;
		
		try {
			createIndex = this.validate();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (createIndex)
			this.createIndex(this.indexPath, this.docPath, this.debug);
	}
	
	private boolean validate() throws Exception {
		if (this.indexPath == null)
			throw new Exception("Non è stata specificato il Path dell'Indice: utilizza il parametro [-index INDEX_PATH]");
		
		final Path indexDir = Paths.get(this.indexPath);
		boolean usableIndexDir = Files.isReadable(indexDir);
		if (!usableIndexDir)
			throw new Exception("La Directory '" + indexDir.toAbsolutePath() + "' non esiste oppure non è accessbile in Lettura");
		
		boolean emptyIndexDir = !(Files.list(indexDir).findAny().isPresent());
		if (emptyIndexDir && this.docPath == null)
			throw new Exception("Non è stato specificato dove recuperare i File da indicizzare: utilizza il parametro [-docs DOCS_PATH]");
		
		final Path docDir = Paths.get(this.docPath);
		boolean usableDocDir = Files.isReadable(docDir);
		if (!usableDocDir)
			throw new Exception("La Directory '" + docDir.toAbsolutePath() + "' non esiste oppure non è accessibile in Lettura");
		
		return emptyIndexDir;
	}
	
	private void createIndex(String indexPath, String docPath, boolean debug) throws Exception {
		Path indexDir = Paths.get(indexPath);
		Directory directory = FSDirectory.open(indexDir);
		
		// TODO: Specificare come lavorare sul Testo per l'Indicizzazione
		/*
		 * Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		 * perFieldAnalyzers.put("titolo", ...);
		 * perFieldAnalyzers.put("contenuto", ...);
		 * IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzers);
		*/
		
		IndexWriterConfig config = new IndexWriterConfig();
		if (debug)
			config.setCodec(new SimpleTextCodec());
		
		IndexWriter writer = new IndexWriter(directory, config);
		List<Document> documents = new ArrayList<>();
		
		File folder = new File(docPath);
		Scanner scannerDaFile = null;
		for (File file: folder.listFiles()) {
			scannerDaFile = new Scanner(file);
			
			String titolo = scannerDaFile.nextLine();
			scannerDaFile.nextLine();
			String contenuto = "";
			
			while (scannerDaFile.hasNextLine()) {
				String s = scannerDaFile.nextLine();
				if (s != null) {
					char lastChar = s.charAt(s.length()-1);
					if (lastChar == '.' || lastChar == '!' || lastChar == '?')
						s += " ";
				}
				contenuto += s;
			}
			
			Document document = new Document();
			document.add(new TextField("titolo", titolo, Field.Store.YES));
			document.add(new TextField("contenuto", contenuto, Field.Store.YES));
			
			documents.add(document);
		}
		
		for (Document d: documents)
			writer.addDocument(d);
		
		writer.commit();
		writer.close();
		directory.close();
	}	
}