package com.twasyl.lat.utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PresentationBuilder {

    /**
     * Represents a slide defined by a template
     */
    public static class Slide {
        private int id;
        private String slideNumber;
        private String name;
        private File file;
        private String text;
        private List<Slide> slides = new ArrayList<>();

        public Slide() {
        }

        public Slide(int id, String name, File file) {
            this.id = id;
            this.name = name;
            this.file = file;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public File getFile() { return file; }
        public void setFile(File file) { this.file = file; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getSlideNumber() { return slideNumber; }
        public void setSlideNumber(String slideNumber) { this.slideNumber = slideNumber; }

        public List<Slide> getSlides() { return slides; }
        public void setSlides(List<Slide> slides) { this.slides = slides; }

        public static void buildContent(StringBuffer buffer, Slide slide) throws IOException, SAXException, ParserConfigurationException {
            if(slide.getSlides().isEmpty()) buffer.append(slide.getText());
        }
    }

    /**
     * Represents the template found in the template configuration file
     */
    public static class Template {
        protected static final String TEMPLATE_CONFIGURATION_NAME = "template-config.xml";
        private static final String TEMPLATE_ROOT = "/slideshow-fx";

        private File folder;
        private File configurationFile;
        private String name;
        private File file;
        private List<Slide> slides;
        private String contentDefinerMethod;
        private String jsObject;
        private File slidesTemplateDirectory;
        private File slidesPresentationDirectory;

        public Template() {
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public File getFile() { return file;  }
        public void setFile(File file) { this.file = file; }

        public List<Slide> getSlides() { return slides; }
        public void setSlides(List<Slide> slides) { this.slides = slides; }

        public File getFolder() { return folder; }
        public void setFolder(File folder) { this.folder = folder; }

        public File getConfigurationFile() { return configurationFile; }
        public void setConfigurationFile(File configurationFile) { this.configurationFile = configurationFile; }

        public String getContentDefinerMethod() { return contentDefinerMethod; }
        public void setContentDefinerMethod(String contentDefinerMethod) { this.contentDefinerMethod = contentDefinerMethod; }

        public String getJsObject() { return jsObject; }
        public void setJsObject(String jsObject) { this.jsObject = jsObject; }

        public File getSlidesTemplateDirectory() { return slidesTemplateDirectory; }
        public void setSlidesTemplateDirectory(File slidesTemplateDirectory) { this.slidesTemplateDirectory = slidesTemplateDirectory; }

        public File getSlidesPresentationDirectory() { return slidesPresentationDirectory; }
        public void setSlidesPresentationDirectory(File slidesPresentationDirectory) { this.slidesPresentationDirectory = slidesPresentationDirectory; }

        /**
         * Read the configuration of this template located in the <b>folder</b> attribute.
         */
        public void readFromFolder() {

            // Set the template information
            LOGGER.fine("Starting reading template configuration");
            this.setConfigurationFile(new File(this.getFolder(), Template.TEMPLATE_CONFIGURATION_NAME));

            XPath xpath = XPathFactory.newInstance().newXPath();
            InputSource configurationFileInput = new InputSource(this.getConfigurationFile().getAbsolutePath());
            String expression = TEMPLATE_ROOT + "/template/name";

            try {
                this.setName(xpath.evaluate(expression, configurationFileInput));
                LOGGER.fine("[Template configuration] name = " + this.getName());

                expression = TEMPLATE_ROOT + "/template/file";
                this.setFile(new File(this.getFolder(), xpath.evaluate(expression, configurationFileInput)));
                LOGGER.fine("[Template configuration] file = " + this.getFile().getAbsolutePath());

                expression = TEMPLATE_ROOT + "/template/js-object";
                this.setJsObject(xpath.evaluate(expression, configurationFileInput));
                LOGGER.fine("[Template configuration] jsObject = " + this.getJsObject());

                expression = TEMPLATE_ROOT + "/template/methods/method[1]/name";
                this.setContentDefinerMethod(xpath.evaluate(expression, configurationFileInput));
                LOGGER.fine("[Template configuration] content definer method = " + this.getContentDefinerMethod());

                // Setting the slides
                this.setSlides(new ArrayList<Slide>());

                LOGGER.fine("Reading slide's configuration");

                expression = TEMPLATE_ROOT + "/slides/template-directory";
                this.setSlidesTemplateDirectory(new File(this.getFolder(), xpath.evaluate(expression, configurationFileInput)));
                LOGGER.fine("[Slide's configuration] template directory = " + this.getSlidesTemplateDirectory().getAbsolutePath());

                expression = TEMPLATE_ROOT + "/slides/presentation-directory";
                this.setSlidesPresentationDirectory(new File(this.getFolder(), xpath.evaluate(expression, configurationFileInput)));
                LOGGER.fine("[Slide's configuration] presentation directory = " + this.getSlidesPresentationDirectory().getAbsolutePath());

                expression = TEMPLATE_ROOT + "/slides/slide";
                NodeList slidesXpath = null;

                slidesXpath = (NodeList) xpath.evaluate(expression, configurationFileInput, XPathConstants.NODESET);

                if(slidesXpath != null && slidesXpath.getLength() > 0) {
                    Node slideNode;
                    Slide slide;

                    for(int index = 0; index < slidesXpath.getLength(); index++) {
                        slideNode = slidesXpath.item(index);
                        slide = new Slide();

                        expression = "id";
                        slide.setId(((Number) xpath.evaluate(expression, slideNode, XPathConstants.NUMBER)).intValue());
                        LOGGER.fine("[Slide configuration] id = " + slide.getId());

                        expression = "name";
                        slide.setName(xpath.evaluate(expression, slideNode));
                        LOGGER.fine("[Slide configuration] name = " + slide.getName());

                        expression = "file";
                        slide.setFile(new File(this.getSlidesTemplateDirectory(), xpath.evaluate(expression, slideNode)));
                        LOGGER.fine("[Slide configuration] file = " + slide.getFile().getAbsolutePath());

                        this.getSlides().add(slide);
                    }
                } else {
                    LOGGER.fine("No slide's configurationfound");
                }
            } catch (XPathExpressionException e) {
                LOGGER.log(Level.WARNING, "Error parsing the template configuration", e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Represents a presentation
     */
    public static class Presentation {
        protected static final String PRESENTATION_CONFIGURATION_NAME = "presentation-config.txt";
        protected static final String PRESENTATION_FILE_NAME = "presentation.html";

        private File presentationFile;
        private List<Slide> slides;

        public File getPresentationFile() { return presentationFile; }
        public void setPresentationFile(File presentationFile) { this.presentationFile = presentationFile; }

        public List<Slide> getSlides() { return slides; }
        public void setSlides(List<Slide> slides) { this.slides = slides; }

        public void updateSlideText(String slideNumber, String content) {
            if(slideNumber == null) throw new IllegalArgumentException("The slide number can not be null");

            Slide slideToUpdate = null;
            for (Slide s : getSlides()) {
                if (slideNumber.equals(s.getSlideNumber())) {
                    s.setText(content);
                    LOGGER.finest("Slide's text updated");
                    break;
                }
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PresentationBuilder.class.getName());
    private static final String VELOCITY_SLIDE_NUMBER_TOKEN = "slideNumber";
    private static final String VELOCITY_SLIDES_TOKEN = "slides";
    private static final String VELOCITY_SFX_CALLBACK_TOKEN = "sfxCallback";
    private static final String VELOCITY_SFX_CONTENT_DEFINER_TOKEN = "sfxContentDefiner";

    private static final String VELOCITY_SFX_CONTENT_DEFINER_SCRIPT = "function setField(slide, what, value) {\n" +
            "\telement = document.getElementById(slide + \"-\" + what);\n" +
            "\telement.innerHTML = value;\n" +
            "}";
    private static final String VELOCITY_SFX_CALLBACK_SCRIPT = "function sendInformationToSlideshowFX(source) {\n" +
            "\tdashIndex = source.id.indexOf(\"-\");\n" +
            "\tslideNumber = source.id.substring(0, dashIndex);\n" +
            "\tfieldName = source.id.substring(dashIndex+1);\n" +
            "\n" +
            "\tsfx.prefillContentDefinition(slideNumber, fieldName, source.innerHTML);\n" +
            "}";

    private static final String VELOCITY_SFX_CALLBACK_CALL = "sendInformationToSlideshowFX(this);";

    private Template template;
    private Presentation presentation;
    private File templateArchiveFile;
    private File presentationArchiveFile;

    public PresentationBuilder() {
    }

    public PresentationBuilder(File template) {
        this.templateArchiveFile = template;
    }

    public File getTemplateArchiveFile() { return this.templateArchiveFile; }
    public void setTemplateArchiveFile(File template) { this.templateArchiveFile = template; }

    public File getPresentationArchiveFile() { return presentationArchiveFile; }
    public void setPresentationArchiveFile(File presentationArchiveFile) { this.presentationArchiveFile = presentationArchiveFile; }

    public Presentation getPresentation() { return this.presentation; }
    public void setPresentation(Presentation presentationFile) { this.presentation = presentationFile; }

    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }

    /**
     * Prepare the resources:
     * <ul>
     *     <li>Create an instance of Template</li>
     *     <li>Create the temporary folder</li>
     *     <li>Extract the data</li>
     *     <li>Load the template data</li>
     * </ul>
     */
    private void prepareResources(File dataArchive) throws IOException {
        if(dataArchive == null) throw new IllegalArgumentException("Can not prepare the resources: the dataArchive is null");
        if(!dataArchive.exists()) throw new IllegalArgumentException("Can not prepare the resources: dataArchive does not exist");

        this.template = new Template();
        this.template.setFolder(new File(System.getProperty("java.io.tmpdir") + File.separator + "sfx-" + System.currentTimeMillis()));

        LOGGER.fine("The temporaryTemplateFolder is " + this.template.getFolder().getAbsolutePath());

        this.template.getFolder().deleteOnExit();

        // Unzip the template into a temporary folder
        LOGGER.fine("Extracting the template ...");

        ZipUtils.unzip(dataArchive, this.template.getFolder());

        // Read the configuration
        this.template.readFromFolder();
    }

    /**
     * Load the current template defined by the templateArchiveFile attribute.
     * This creates a temporary file.
     */
    public void loadTemplate() throws IOException {
        this.prepareResources(this.templateArchiveFile);

        // Copy the template to the presentation file
        LOGGER.fine("Creating presentation file");
        this.presentation = new Presentation();
        this.presentation.setSlides(new ArrayList<Slide>());
        this.presentation.setPresentationFile(new File(this.template.getFolder(), Presentation.PRESENTATION_FILE_NAME));

        // Replacing the velocity tokens
        final Reader templateReader = new FileReader(this.template.getFile());
        final Writer presentationWriter = new FileWriter(this.presentation.getPresentationFile());

        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put(VELOCITY_SFX_CONTENT_DEFINER_TOKEN, VELOCITY_SFX_CONTENT_DEFINER_SCRIPT);
        context.put(VELOCITY_SFX_CALLBACK_TOKEN, VELOCITY_SFX_CALLBACK_SCRIPT);
        context.put(VELOCITY_SLIDES_TOKEN, "");

        Velocity.evaluate(context, presentationWriter, "", templateReader);

        presentationWriter.flush();
        presentationWriter.close();

        templateReader.close();

        LOGGER.fine("Presentation file created at " + this.presentation.getPresentationFile().getAbsolutePath());
    }

    /**
     * Load the given template
     * @param template
     * @throws IOException
     */
    public void loadTemplate(File template) throws IOException {
        setTemplateArchiveFile(template);
        this.loadTemplate();
    }

    /**
     * Open a saved presentation
     */
    public void openPresentation() throws IOException, ParserConfigurationException, SAXException {
        this.prepareResources(this.templateArchiveFile);
        this.presentationArchiveFile = this.templateArchiveFile;

        // Copy the template to the presentation file
        LOGGER.fine("Creating presentation file");
        this.presentation = new Presentation();
        this.presentation.setSlides(new ArrayList<Slide>());
        this.presentation.setPresentationFile(new File(this.template.getFolder(), Presentation.PRESENTATION_FILE_NAME));

        // Reading the slides' configuration
        LOGGER.fine("Parsing presentation configuration");

        JSONObject configuration = (JSONObject) JSONValue.parse(new FileInputStream(new File(this.template.getFolder(), Presentation.PRESENTATION_CONFIGURATION_NAME)));
        JSONObject presentationJson = (JSONObject) configuration.get("presentation");
        JSONArray slidesJson = (JSONArray) presentationJson.get("slides");
        JSONObject slideJson;
        Slide slide;

        LOGGER.fine("Reading slides configuration");
        for(int index = 0; index < slidesJson.size(); index++)  {
            slide = new Slide();

            slideJson = (JSONObject) slidesJson.get(index);
            slide.setSlideNumber((String) slideJson.get("number"));
            slide.setId((Integer) slideJson.get("template-id"));
            slide.setFile(new File(this.template.getSlidesPresentationDirectory(), (String) slideJson.get("file")));

            this.presentation.getSlides().add(slide);
        }

        // Fill the slides' content
        LOGGER.fine("Reading slides files");
        FileInputStream slideInput = null;
        ByteArrayOutputStream slideContent = null;
        byte[] buffer = new byte[1024];
        int length;
        StringBuffer slidesBuffer = new StringBuffer();

        for(Slide s : this.presentation.getSlides()) {
            LOGGER.fine("Reading slide file: " + s.getFile().getAbsolutePath());
            try {
                slideInput = new FileInputStream(s.getFile());
                slideContent = new ByteArrayOutputStream();

                while((length = slideInput.read(buffer)) > 0) {
                    slideContent.write(buffer, 0, length);
                }
            } catch(IOException ex) {
                LOGGER.log(Level.WARNING, "Error while reading slide content", ex);
            } finally {
                if(slideContent != null) {
                    s.setText(new String(slideContent.toByteArray(), Charset.forName("UTF-8")));
                    Slide.buildContent(slidesBuffer, s);

                    try {
                        slideContent.close();
                    }
                    catch(IOException ex) {
                        LOGGER.log(Level.WARNING, "Can not close slide content stream");
                    }
                }
                if(slideInput != null) {
                    try {
                        slideInput.close();
                    } catch(IOException ex) {
                        LOGGER.log(Level.WARNING, "Can not close slide content file");
                    }
                }
            }

        }

        // Replacing the velocity tokens
        LOGGER.fine("Building presentation file");
        final Reader templateReader = new FileReader(this.template.getFile());
        final Writer presentationWriter = new FileWriter(this.presentation.getPresentationFile());

        Velocity.init();
        VelocityContext context = new VelocityContext();
        context.put(VELOCITY_SFX_CONTENT_DEFINER_TOKEN, VELOCITY_SFX_CONTENT_DEFINER_SCRIPT);
        context.put(VELOCITY_SFX_CALLBACK_TOKEN, VELOCITY_SFX_CALLBACK_SCRIPT);
        context.put(VELOCITY_SLIDES_TOKEN, slidesBuffer.toString());

        Velocity.evaluate(context, presentationWriter, "", templateReader);

        presentationWriter.flush();
        presentationWriter.close();

        templateReader.close();

        LOGGER.fine("Presentation file created at " + this.presentation.getPresentationFile().getAbsolutePath());
    }

    public void openPresentation(File presentation) throws IOException, ParserConfigurationException, SAXException {
        setTemplateArchiveFile(presentation);
        openPresentation();
    }

    /**
     * Add a slide to the presentation and save the presentation
     * @param template
     * @throws IOException
     */
    public void addSlide(Slide template, Slide parent) throws IOException, ParserConfigurationException, SAXException {
        if(template == null) throw new IllegalArgumentException("The template for creating a slide can not be null");
        Velocity.init();

        final Slide slide = new Slide(template.getId(), template.getName(), template.getFile());
        slide.setSlideNumber(template.getSlideNumber());

        if(parent == null) {
            slide.setSlideNumber((this.presentation.getSlides().size() + 1) + "");
            this.presentation.getSlides().add(slide);
        } else {
           slide.setSlideNumber(parent.getSlideNumber() + "." + parent.getSlides().size() + 1);
        }

        final Reader slideFileReader = new FileReader(slide.getFile());
        final ByteArrayOutputStream slideContentByte = new ByteArrayOutputStream();
        final Writer slideContentWriter = new OutputStreamWriter(slideContentByte);

        VelocityContext context = new VelocityContext();
        context.put(VELOCITY_SLIDE_NUMBER_TOKEN, slide.getSlideNumber());
        context.put(VELOCITY_SFX_CALLBACK_TOKEN, VELOCITY_SFX_CALLBACK_CALL);

        Velocity.evaluate(context, slideContentWriter, "", slideFileReader);
        slideContentWriter.flush();
        slideContentWriter.close();

        slide.setText(new String(slideContentByte.toByteArray()));

        this.saveTemporaryPresentation();
    }

    public void saveTemporaryPresentation() throws ParserConfigurationException, SAXException, IOException {
        Velocity.init();

        // Saving the presentation file
        // Step 1: build the slides
        final StringBuffer slidesBuffer = new StringBuffer();
        for(Slide s : this.presentation.getSlides()) {
            Slide.buildContent(slidesBuffer, s);
        }

        // Step 2: rewrite from the template
        final Reader templateReader = new FileReader(this.template.getFile());
        final Writer presentationWriter = new FileWriter(this.presentation.getPresentationFile());

        VelocityContext context = new VelocityContext();
        context.put(VELOCITY_SFX_CONTENT_DEFINER_TOKEN, VELOCITY_SFX_CONTENT_DEFINER_SCRIPT);
        context.put(VELOCITY_SFX_CALLBACK_TOKEN, VELOCITY_SFX_CALLBACK_SCRIPT);
        context.put(VELOCITY_SLIDES_TOKEN, slidesBuffer.toString());

        Velocity.evaluate(context, presentationWriter, "", templateReader);

        templateReader.close();
        presentationWriter.flush();
        presentationWriter.close();
    }

    /**
     * Build the final presentation file by writing the complete package
     */
    public void savePresentation(File presentationArchive) throws IOException {
        if(presentationArchive == null) throw new IllegalArgumentException("The presentation archive can not be null");

        LOGGER.fine("Creating the presentation configuration file");
        JSONArray slidesJsonArray = new JSONArray();
        JSONObject slideJson;

        for(Slide slide : this.presentation.getSlides()) {
            slideJson = new JSONObject();
            slideJson.put("template-id", slide.getId());
            slideJson.put("number", slide.getSlideNumber());
            slideJson.put("file", slide.getSlideNumber() + ".html");
            slidesJsonArray.add(slideJson);
        }

        JSONObject presentationJson = new JSONObject();
        presentationJson.put("slides", slidesJsonArray);

        JSONObject configuration = new JSONObject();
        configuration.put("presentation", presentationJson);

        PrintWriter writer = new PrintWriter(new File(this.template.getFolder(), Presentation.PRESENTATION_CONFIGURATION_NAME));
        writer.print(configuration.toJSONString(JSONStyle.NO_COMPRESS));
        writer.flush();
        writer.close();

        LOGGER.fine("Presentation configuration file created");

        LOGGER.fine("Creating slides files");
        if(!this.template.getSlidesPresentationDirectory().exists()) this.template.getSlidesPresentationDirectory().mkdirs();

        PrintWriter slideWriter = null;
        for(Slide slide : this.presentation.getSlides()) {
            LOGGER.fine("Creating file: " + this.template.getSlidesPresentationDirectory().getAbsolutePath() + File.separator + slide.getSlideNumber() + ".html");
            slideWriter = new PrintWriter(new File(this.template.getSlidesPresentationDirectory(), slide.getSlideNumber() + ".html"));
            slideWriter.print(slide.getText());
            slideWriter.flush();
            slideWriter.close();
        }

        LOGGER.fine("Slides files created");

        LOGGER.fine("Compressing temporary file");
        ZipUtils.zip(this.template.getFolder(), presentationArchive);
        LOGGER.fine("Presentation saved");
    }
}
