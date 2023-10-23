package lucene.sw;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class QueryHandler {
	
	private String indexPath;
	private Directory directory;
	private IndexReader reader;
	private IndexSearcher searcher;
	
	public QueryHandler(String indexPath) throws Exception {
		this.indexPath = indexPath;
		
		Path indexDir = Paths.get(this.indexPath);
		this.directory = FSDirectory.open(indexDir);
		
		this.reader = DirectoryReader.open(this.directory);
		this.searcher = new IndexSearcher(this.reader);
	}
	
	public void execute() throws Exception {
		System.out.println("\nQUI PUOI INSERIRE LE TUE QUERY");
		
		Scanner scanner = new Scanner(System.in);
		while(true) {
			System.out.print("Inserisci la Query, oppure \"stop\" per terminare l'esecuzione: ");
			String s = scanner.nextLine();
			if (s.equals("stop")) {
				System.out.println("Fine dell'Esecuzione\n\n");
				scanner.close();
				break;
			}
			else
				this.runQuery(s);
		}
		
		this.directory.close();
	}
	
	private void runQuery(String query) throws Exception {
		Map<String, Float> weights = new HashMap<>();
		float titleWeight = 1;
		float contentWeight = (float)0.75;
		weights.put("titolo", titleWeight);
		weights.put("contenuto", contentWeight);
		
		Analyzer queryAnalyzer = CustomAnalyzer.builder()
								.withTokenizer(StandardTokenizerFactory.class)
								.addTokenFilter(LowerCaseFilterFactory.class)
								.build();
		
		SimpleQueryParser parser = new SimpleQueryParser(queryAnalyzer, weights);
		
		Query parsedQuery = parser.parse(query);
		
		TopDocs hits = searcher.search(parsedQuery, 10);
		for (int i=0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = searcher.doc(scoreDoc.doc);
			
			String titolo = doc.get("titolo");
			String contenuto = doc.get("contenuto");
			
			System.out.printf("DOCUMENTO IN POSIZIONE: %d\n", i);
			System.out.println("- Titolo: " + titolo);
			System.out.println("- Contenuto: " + contenuto);
			System.out.println("- Punteggio: " + scoreDoc.score + "\n");
		}
	}
}