/*
 * Copyright 2016 Thierry Wasylczenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twasyl.slideshowfx.content.extension.quiz.controllers;

import com.twasyl.slideshowfx.beans.quiz.Answer;
import com.twasyl.slideshowfx.beans.quiz.Question;
import com.twasyl.slideshowfx.beans.quiz.Quiz;
import com.twasyl.slideshowfx.content.extension.AbstractContentExtensionController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * This class is the controller used by the {@code QuoteContentExtension.fxml} file.
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX 1.0.0
 */
public class QuizContentExtensionController extends AbstractContentExtensionController {

    @FXML private VBox answersBox;
    @FXML private TextArea question;
    @FXML private Button addAnswer = new Button();

    private final ObjectProperty<Quiz> quiz = new SimpleObjectProperty<>();

    /**
     * Add an answer to this {@link #quiz}. This method creates an {@link com.twasyl.slideshowfx.beans.quiz.Answer}
     * object and binds it to the elements that are used to specify the text of it and if it is considered as a right
     * answer.
     * The method also updates this panel with elements used to define the answer. The answer is also added to this
     * quiz.
     */
    @FXML private void addAnswer(final ActionEvent event) {
        this.addAnswer();
    }

    private void addAnswer() {
        final Answer answer = new Answer();
        answer.setQuiz(this.quiz.get());
        answer.setId(System.currentTimeMillis());

        this.quiz.get().getAnswers().add(answer);

        final TextField answerText = new TextField();
        answerText.setPromptText("Answer");
        answerText.setPrefColumnCount(25);
        answerText.setTooltip(new Tooltip("Enter the text for this answer"));
        answerText.textProperty().bindBidirectional(answer.textProperty());

        final CheckBox isCorrect = new CheckBox();
        isCorrect.setTooltip(new Tooltip("Check if this answer is considered as a correct answer"));
        isCorrect.selectedProperty().bindBidirectional(answer.correctProperty());

        if(this.addAnswer.getParent() != null) {
            super.executeUnderFXThread(() -> ((HBox) this.addAnswer.getParent()).getChildren().remove(this.addAnswer));
        }

        final HBox answerContainer = new HBox(5, answerText, isCorrect, this.addAnswer);
        answerContainer.setAlignment(Pos.CENTER_LEFT);

        super.executeUnderFXThread(() -> this.answersBox.getChildren().add(answerContainer));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.initializeQuiz();
        this.addAnswer();
    }

    /**
     * Initialize the {@link #quiz} property.
     */
    private void initializeQuiz() {
        this.quiz.set(new Quiz());
        this.quiz.get().setId(System.currentTimeMillis());

        // Initialize the question
        this.quiz.get().setQuestion(new Question());
        this.quiz.get().getQuestion().setQuiz(this.quiz.get());
        this.quiz.get().getQuestion().setId(System.currentTimeMillis());

        question.textProperty().bindBidirectional(this.quiz.get().getQuestion().textProperty());
    }

    /**
     * Get the {@link Quiz} associated to this controller. The quiz is fully initialized and doesn't need further configuration.
     * The quiz is never null.
     * @return The property containing the quiz.
     */
    public ObjectProperty<Quiz> quizProperty() { return quiz; }

    /**
     * Get the {@link Quiz} associated to this controller. The quiz is fully initialized and doesn't need further configuration.
     * The quiz is never null.
     * @return The quiz itself.
     */
    public Quiz getQuiz() {  return quiz.get(); }
}
