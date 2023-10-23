package lucene.sw;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
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
		else
			System.out.println("Verrà utilizzato l'Indice esistente in: " + indexPath);
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

		/* Specifica di come lavorare sul Testo prima dell'Indicizzazione */
		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		
		// Preprocessing del Titolo
		Analyzer titleAnalyzer = CustomAnalyzer.builder()
								.withTokenizer(WhitespaceTokenizerFactory.class)
								.addTokenFilter(LowerCaseFilterFactory.class)
								.addTokenFilter(WordDelimiterGraphFilterFactory.class)
								.build();
		
		// Pre-processing del Contenuto
		CharArraySet stopWords = new CharArraySet(Arrays.asList("e","il","lo","la","i","gli","le","un","uno","una","che"), true);
		Analyzer contentAnalyzer = new StandardAnalyzer(stopWords);
		
		perFieldAnalyzers.put("titolo", titleAnalyzer);
		perFieldAnalyzers.put("contenuto", contentAnalyzer);
		
		PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(null, perFieldAnalyzers);
		IndexWriterConfig config = new IndexWriterConfig(analyzerWrapper);
		if (debug)
			config.setCodec(new SimpleTextCodec());

		IndexWriter writer = new IndexWriter(directory, config);
		List<Document> documents = new ArrayList<>();

		/* Lettura dei Documenti */
		File folder = new File(docPath);
		Scanner scannerDaFile = null;
		
		float start = Instant.now().toEpochMilli();
		for (File file: folder.listFiles()) {
			scannerDaFile = new Scanner(file);

			String titolo = scannerDaFile.nextLine();	// La prima Riga del Documento è il Titolo
			scannerDaFile.nextLine();					// La seconda Riga è vuota: usata per separare Titolo e Contenuto
			String contenuto = "";

			// Il Contenuto del Documento è tutto ciò che segue il Titolo, fino alla fine del Documento stesso
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
		
		float end = Instant.now().toEpochMilli();
		float time = (end-start)/1000;
		System.out.println("E' stato creato un nuovo Indice in: " + indexPath);
		System.out.println(" - Tempo necessario per la Creazione: " + time + " secondi");
		System.out.println(" - Documenti Indicizzati: " + folder.listFiles().length);
		
		writer.close();
		directory.close();
	}	
}