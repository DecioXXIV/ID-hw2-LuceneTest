package lucene.sw;

public class Main {
	
	/* Lanciare il Programma passando i seguenti parametri al main():
	 * [-index INDEX_PATH] -> Path della Directory dell'Indice Lucene
	 * [-docs DOCS_PATH]   -> Path della Directory da dove vengono recuperati i Documenti .txt da indicizzare
	 * [-debug]			   -> Opzionale, per imporre la creazione dell'Indice Lucene come file testuale e non binario, utile per il Debugging
	 */
	public static void main(String[] args) throws Exception {
		String indexPath = null;
		String docsPath = null;
		boolean debug = false;
		
		for (int i=0; i < args.length; i++) {
			switch(args[i]) {
			case "-index":
				indexPath = args[++i];
				break;
			case "-docs":
				docsPath = args[++i];
				break;
			case "-debug":
				debug = true;
				break;
			default:
				throw new IllegalArgumentException("Parametro Sconosciuto " + args[i]);
			}
		}
		
		System.out.println("*** LUCENE TESTING ***");
		IndexHandler indexHandler = new IndexHandler(indexPath, docsPath, debug);
		
		// TODO: Gestione delle Query, inserite da input
		/* QueryHandler queryHandler = new QueryHandler(...);
		 * ...
		 */
	}
}