package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.apache.ctakes.core.util.PropertyAeFactory;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a pipeline using a small set of simple methods.
 * <p>
 * Some methods are order-specific and calls will directly impact ordering within the pipeline.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2016
 */
final public class PipelineBuilder {

   static private final Logger LOGGER = Logger.getLogger( "PipelineBuilder" );

   private CollectionReaderDescription _readerDesc;
   private final List<String> _aeNameList;
   private final List<AnalysisEngineDescription> _descList;
   private final List<String> _aeEndNameList;
   private final List<AnalysisEngineDescription> _descEndList;
   // Allow the pipeline to be changed even after it has been built once.
   private AnalysisEngineDescription _analysisEngineDesc;
   private boolean _pipelineChanged;


   public PipelineBuilder() {
      _aeNameList = new ArrayList<>();
      _descList = new ArrayList<>();
      _aeEndNameList = new ArrayList<>();
      _descEndList = new ArrayList<>();
   }

   public void clear() {
      _aeNameList.clear();
      _descList.clear();
      _aeEndNameList.clear();
      _descEndList.clear();
   }

   /**
    * Use of this method is order-specific
    *
    * @param parameters add ae parameter name value pairs
    * @return this PipelineBuilder
    */
   public PipelineBuilder set( final Object... parameters ) {
      PropertyAeFactory.getInstance().addParameters( parameters );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is not order-specific
    *
    * @param description Collection Reader Description to place at the beginning of the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder reader( final CollectionReaderDescription description ) {
      _readerDesc = description;
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is not order-specific
    *
    * @param readerClass Collection Reader class to place at the beginning of the pipeline
    * @param parameters reader parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    */
   public PipelineBuilder reader( final Class<? extends CollectionReader> readerClass, final Object... parameters )
         throws UIMAException {
      reader( CollectionReaderFactory.createReaderDescription( readerClass, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Relies upon {@link org.apache.ctakes.core.config.ConfigParameterConstants#PARAM_INPUTDIR} having been specified
    * Use of this method is not order-specific.
    *
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles() throws UIMAException {
      _readerDesc = CollectionReaderFactory.createReaderDescription( FileTreeReader.class );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Use of this method is not order-specific
    *
    * @param inputDirectory directory with input files
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles( final String inputDirectory ) throws UIMAException {
      _readerDesc = CollectionReaderFactory.createReaderDescription( FileTreeReader.class,
            ConfigParameterConstants.PARAM_INPUTDIR,
            inputDirectory );
      _pipelineChanged = true;
      return this;
   }

   /**
    *
    * @return the Collection Reader for the pipeline or null if none has been specified
    */
   public CollectionReaderDescription getReader() {
      return _readerDesc;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder add( final Class<? extends AnalysisComponent> component,
                               final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _descList.add( PropertyAeFactory.getInstance().createDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds an ae or cc wrapped with "Starting processing" and "Finished processing" log messages
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder addLogged( final Class<? extends AnalysisComponent> component,
                                     final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _descList.add( PropertyAeFactory.getInstance().createLoggedDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param description ae or cc component class description to add to the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder addDescription( final AnalysisEngineDescription description ) {
      _aeNameList.add( description.getAnnotatorImplementationName() );
      _descList.add( description );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds an ae or cc component t othe very end of the pipeline.  Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the end of the pipeline
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder addLast( final Class<? extends AnalysisComponent> component,
                                   final Object... parameters ) throws ResourceInitializationException {
      _aeEndNameList.add( component.getName() );
      _descEndList.add( PropertyAeFactory.getInstance().createDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    *
    * @return an ordered list of the annotation engines in the pipeline
    */
   public List<String> getAeNames() {
      final List<String> allNames = new ArrayList<>( _aeNameList );
      allNames.addAll( _aeEndNameList );
      return Collections.unmodifiableList( allNames );
   }

   /**
    * Adds ae that maintains CUI information throughout the run.
    * CUI information can later be accessed using the {@link CuiCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the CuiCollector engine cannot be created
    */
   public PipelineBuilder collectCuis() throws ResourceInitializationException {
      return add( CuiCollector.CuiCollectorEngine.class );
   }

   /**
    * Adds ae that maintains simple Entity information throughout the run.
    * Entity information can later be accessed using the {@link EntityCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the EntityCollector engine cannot be created
    */
   public PipelineBuilder collectEntities() throws ResourceInitializationException {
      return add( EntityCollector.EntityCollectorEngine.class );
   }

   /**
    * Adds ae that writes an xmi file at the end of the pipeline.
    * Relies upon {@link ConfigParameterConstants#PARAM_OUTPUTDIR} having been specified
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs() throws ResourceInitializationException {
      return addLast( FileTreeXmiWriter.class );
   }

   /**
    * Adds ae that writes an xmi file at the end of the pipeline.
    * Use of this method is order-specific.
    *
    * @param outputDirectory directory in which xmi files should be written
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs( final String outputDirectory ) throws ResourceInitializationException {
      return addLast( FileTreeXmiWriter.class, ConfigParameterConstants.PARAM_OUTPUTDIR, outputDirectory );
   }

   /**
    * Initialize a pipeline that can be used repeatedly using {@link #run} and {@link #run(String)}.
    * A pipeline can be extended between builds, but the full pipeline will be rebuilt on each call.
    * Use of this method is order-specific.
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be built
    * @throws UIMAException if the pipeline could not be built
    */
   public PipelineBuilder build() throws IOException, UIMAException {
      if ( _analysisEngineDesc == null || _pipelineChanged ) {
         final AggregateBuilder builder = new AggregateBuilder();
         _descList.forEach( builder::add );
         _descEndList.forEach( builder::add );
         _analysisEngineDesc = builder.createAggregateDescription();
      }
      _pipelineChanged = false;
      return this;
   }

   /**
    * Run the pipeline using some specified collection reader.
    * Use of this method is order-specific.
    * This method will call {@link #build()} if the pipeline has not already been initialized.
    *
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run() throws IOException, UIMAException {
      if ( _readerDesc == null ) {
         LOGGER.error( "No Collection Reader specified." );
         return this;
      }
      build();
      SimplePipeline.runPipeline( _readerDesc, _analysisEngineDesc );
      return this;
   }

   /**
    * Run the pipeline on the given text.
    * Use of this method is order-specific.
    * This method will call {@link #build()} if the pipeline has not already been initialized.
    *
    * @param text text upon which to run this pipeline
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run( final String text ) throws IOException, UIMAException {
      if ( _readerDesc != null ) {
         LOGGER.error( "Collection Reader specified, ignoring." );
         return this;
      }
      final JCas jcas = JCasFactory.createJCas();
      jcas.setDocumentText( text );
      build();
      SimplePipeline.runPipeline( jcas, _analysisEngineDesc );
      return this;
   }

   /**
    * @return an analysis engine description, for use in creating xml descriptor files, etc.
    * @throws IOException   if the description could not be built
    * @throws UIMAException if the description could not be built
    */
   public AnalysisEngineDescription getAnalysisEngineDesc() throws IOException, UIMAException {
      build();
      return _analysisEngineDesc;
   }

}
