
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2016, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package km;

//import java.text.NumberFormat;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.carrot2.core.Cluster;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;
import org.carrot2.core.attribute.CommonAttributesDescriptor;

/**
 * Simple console formatter for dumping {@link ProcessingResult}.
 */
public class Results {
	public static void displayResults(ProcessingResult processingResult) {
		final Collection<Document> documents = processingResult.getDocuments();
		final Collection<Cluster> clusters = processingResult.getClusters();
		final Map<String, Object> attributes = processingResult.getAttributes();


		// Show documents
		if (documents != null)
		{
			displayDocuments(documents);
		}

		// Show clusters
		if (clusters != null)
		{
			displayClusters(clusters);
		}

		// Show attributes other attributes
		displayAttributes(attributes);
	}

	public static void displayDocuments(final Collection<Document> documents)
	{
		println ("CollectedXX " + documents.size() + " documents\n");
		documents.take(7).each { displayDocument(0, it) }
	}

	public static void displayAttributes(final Map<String, Object> attributes)
	{
		System.out.println("Attributes:");

		String DOCUMENTS_ATTRIBUTE = CommonAttributesDescriptor.Keys.DOCUMENTS;
		String CLUSTERS_ATTRIBUTE = CommonAttributesDescriptor.Keys.CLUSTERS;
		for (final Map.Entry<String, Object> attribute : attributes.entrySet())
		{
			if (!DOCUMENTS_ATTRIBUTE.equals(attribute.getKey())
			&& !CLUSTERS_ATTRIBUTE.equals(attribute.getKey()))
			{
				System.out.println(attribute.getKey() + ":   " + attribute.getValue());
			}
		}
	}

	public static void displayClusters(final Collection<Cluster> clusters)
	{
		// displayClusters(clusters, 20);//Integer.MAX_VALUE);
		clusterF1(clusters)
	}

	public static void clusterF1(Collection<Cluster> clusters){

		println "clusters $clusters"

		def catTotals = [:]
		def f1list = []

		//get category totals
		clusters.eachWithIndex{ cluster, ind ->
			cluster.getDocuments().eachWithIndex {doc, i ->

				def cat = doc.getField("category")
				def v = catTotals.get((cat)) ?: 0
				catTotals.put((cat), v + 1)
			}
		}

		println "catTotals $catTotals"

		clusters.eachWithIndex{ cluster, ind ->
			println "cluster size " + cluster.size() + " ******************************************************************************"

			def catsFreq=[:]

			cluster.getDocuments().eachWithIndex {doc, i ->

				def cat = doc.getField("category")
				def n = catsFreq.get((cat)) ?: 0
				catsFreq.put((cat), n + 1)
			}

			def catMax = catsFreq?.max{it?.value} ?:0
			println "catsFreq $catsFreq cats max: $catMax  clustersSize: " + cluster.size() + " category total " + catTotals[catMax.key]
			def precision = catMax.value /cluster.size()
			def recall = catMax.value/ catTotals[catMax.key]
			def f1 = (2 * precision * recall) / (precision + recall);
			f1list << f1

			println "recall $recall precision $precision f1 $f1"
			println ""
		}
		def averagef1 = f1list.sum()/f1list.size()
		println "f1list: $f1list averagef1: :$averagef1"
		println "~~~~~~~~~~~~~~~~~"
	}

	public static void displayClusters(final Collection<Cluster> clusters,
			int maxNumberOfDocumentsToShow)
	{
		displayClusters(clusters, maxNumberOfDocumentsToShow,
				ClusterDetailsFormatter.INSTANCE);
	}

	public static void displayClusters(final Collection<Cluster> clusters,
			int maxNumberOfDocumentsToShow, ClusterDetailsFormatter clusterDetailsFormatter)
	{
		System.out.println("\n\nCreated " + clusters.size() + " clusters\n");
		int clusterNumber = 1;
		for (final Cluster cluster : clusters)
		{
			displayCluster(0, "" + clusterNumber++, cluster, maxNumberOfDocumentsToShow,
					clusterDetailsFormatter);
		}
	}

	private static void displayDocument(final int level, Document document)
	{
		final String indent = getIndent(level);

		System.out.printf(indent + "[%2s] ", document.getStringId());
		System.out.println("title " + (Object) document.getField(Document.TITLE));
		System.out.println("category " + (Object) document.getField("category"));
		//	System.out.println("contents " + (Object) document.getField("contents"));

		final String url = document.getField(Document.CONTENT_URL);
		final String cat = document.getField("category");
		if (StringUtils.isNotBlank(url))
		{
			System.out.println(indent + " urlx     " + url);
			System.out.println(indent + " catx     " + cat);
		}
		System.out.println();
	}

	private static void displayCluster(final int level, String tag, Cluster cluster,
			int maxNumberOfDocumentsToShow, ClusterDetailsFormatter clusterDetailsFormatter)
	{
		final String label = cluster.getLabel();

		// indent up to level and display this cluster's description phrase
		for (int i = 0; i < level; i++)
		{
			System.out.print("  ");
		}
		System.out.println(label + "  "
				+ clusterDetailsFormatter.formatClusterDetails(cluster));

		// if this cluster has documents, display three topmost documents.
		int documentsShown = 0;
		for (final Document document : cluster.getDocuments())
		{
			if (documentsShown >= maxNumberOfDocumentsToShow)
			{
				break;
			}
			displayDocument(level + 1, document);
			documentsShown++;
		}
		if (maxNumberOfDocumentsToShow > 0
		&& (cluster.getDocuments().size() > documentsShown))
		{
			System.out.println(getIndent(level + 1) + "... and "
					+ (cluster.getDocuments().size() - documentsShown) + " more\n");
		}

		// finally, if this cluster has subclusters, descend into recursion.
		final int num = 1;
		for (final Cluster subcluster : cluster.getSubclusters())
		{
			displayCluster(level + 1, tag + "." + num, subcluster,
					maxNumberOfDocumentsToShow, clusterDetailsFormatter);
		}
	}

	private static String getIndent(final int level)
	{
		final StringBuilder indent = new StringBuilder();
		for (int i = 0; i < level; i++)
		{
			indent.append("  ");
		}

		return indent.toString();
	}

	public static class ClusterDetailsFormatter
	{
		public final static ClusterDetailsFormatter INSTANCE = new ClusterDetailsFormatter();

	//	protected NumberFormat numberFormat;

		public ClusterDetailsFormatter()
		{
			numberFormat = NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(2);
		}

		public String formatClusterDetails(Cluster cluster)
		{
			final Double score = cluster.getScore();
			return "(" + cluster.getAllDocuments().size() + " docs"
			+ (score != null ? ", score: " + numberFormat.format(score) : "") + ")";
		}
	}
}
