package gov.hhs.aspe.nlp.SafetySurveillance.CoreNLP;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * This class contains cTakes clinical pipeline without the temporal modules and
 * wrap its function with a single API.
 * 
 * @author Wei.Chen
 *
 */
public class CTakesClinical {

	JCas jcas = null;
	String narrative = null;
	AggregateBuilder aggregateBuilder = null;
	AnalysisEngineDescription aed = null;
	AnalysisEngine multipleAE = null;

	/**
	 * This constructor accepts an input string - the raw text.
	 * 
	 */
	public CTakesClinical() throws ResourceInitializationException, MalformedURLException
	{

		aggregateBuilder = new AggregateBuilder();

		aggregateBuilder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
		aggregateBuilder.add(SentenceDetector.createAnnotatorDescription());
		aggregateBuilder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
		aggregateBuilder.add(LvgAnnotator.createAnnotatorDescription());
		aggregateBuilder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
		aggregateBuilder.add(POSTagger.createAnnotatorDescription());
		aggregateBuilder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());
		aggregateBuilder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ClearNLPSemanticRoleLabelerAE.class));
		aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ConstituencyParser.class));
		multipleAE = aggregateBuilder.createAggregate();

	}

	/**
	 * This function accepts a few parameters, applies the analysis engine(s),
	 * and returns the CAS as result.
	 * 
	 * @param doc
	 *            The narrative as a String
	 * @return the annotation results stored in a CAS
	 * @throws AnalysisEngineProcessException
	 * @throws IOException
	 * @throws ResourceInitializationException
	 */
	public JCas processDocument(String s) { // throws

		try {
			this.jcas = multipleAE.newJCas();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
		this.narrative = s;
		jcas.setDocumentText(s);
		try {
			SimplePipeline.runPipeline(this.jcas, multipleAE);
		} catch (AnalysisEngineProcessException e) {
			e.printStackTrace();
		}

		return this.jcas;
	}

	/**
	 * This function returns the annotation result in Pretty Pretty format as a
	 * String.
	 * 
	 * @return annottionResult String
	 */
	public String getResultInPrettyPrint() {
		PrettyTextWriterVaers ptWriter = new PrettyTextWriterVaers();
		ptWriter.process(this.jcas);
		String annotationResult = null;
		try {
			annotationResult = ptWriter.readFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return annotationResult;
	}

}
