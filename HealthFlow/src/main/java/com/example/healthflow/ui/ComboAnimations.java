package com.example.healthflow.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.function.Function;

public final class ComboAnimations {

    private ComboAnimations() {}

    /** تركيب أنيميشن افتراضي (بدون تأخير بين العناصر) */
    public static <T> void applySmoothSelect(ComboBox<T> combo, Function<T, String> toText) {
        applySmoothSelect(combo, toText, Duration.ZERO);
    }

    /** تركيب أنيميشن مع تأخير بين الانتقال من عنصر لعنصر داخل القائمة */
    public static <T> void applySmoothSelect(ComboBox<T> combo,
                                             Function<T, String> toText,
                                             Duration perItemDelay) {
        if (combo == null) return;

        // زرّ الحقل العلوي
        combo.setButtonCell(new PlainCell<>(toText));

        // خلايا القائمة المنسدلة مع الأنيميشن + التأخير
        combo.setCellFactory(cb -> new AnimatedCell<>(toText, perItemDelay == null ? Duration.ZERO : perItemDelay));
    }

    /** تأخير إغلاق القائمة المنسدلة (لـ 1–2 ثانية مثلًا) بعد الاختيار */
    public static <T> void delayHideOnSelect(ComboBox<T> combo, Duration delay) {
        if (combo == null || delay == null) return;

        javafx.scene.control.skin.ComboBoxListViewSkin<T> skin =
                new javafx.scene.control.skin.ComboBoxListViewSkin<>(combo) {
                    @Override public void hide() {
                        PauseTransition pt = new PauseTransition(delay);
                        pt.setOnFinished(e -> super.hide());
                        pt.play();
                    }
                };
        combo.setSkin(skin);
    }

    /** خلية بسيطة لزرّ الـ ComboBox */
    private static final class PlainCell<T> extends ListCell<T> {
        private final Function<T, String> toText;
        PlainCell(Function<T, String> toText) { this.toText = toText; }
        @Override protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? "" : toText.apply(item));
        }
    }

    /** فعّل شريط تمييز منزلق يتحرك بين العناصر داخل popup */
    public static <T> void enableSlidingSelection(ComboBox<T> combo, Duration slideDur) {
        if (combo == null) return;
        final Duration dur = (slideDur == null ? Duration.millis(260) : slideDur);

        combo.showingProperty().addListener((o, was, is) -> {
            if (!is) return; // لما تفتح القائمة
            Platform.runLater(() -> attachOrUpdateSlidingOverlay(combo, dur, true));
        });

        combo.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (!combo.isShowing()) return;
            Platform.runLater(() -> attachOrUpdateSlidingOverlay(combo, dur, false));
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void attachOrUpdateSlidingOverlay(ComboBox<T> combo, Duration dur, boolean retryIfMissing) {
        try {
            var skin = (javafx.scene.control.skin.ComboBoxListViewSkin<T>) combo.getSkin();
            if (skin == null) return;
            ListView<T> lv = (ListView<T>) skin.getPopupContent();
            if (lv == null) return;

            Node vfNode = lv.lookup(".virtual-flow");
            Pane hostPane = null;
            if (vfNode instanceof Pane p) hostPane = p;
            else if (vfNode != null && vfNode.getParent() instanceof Pane p2) hostPane = p2;
            if (hostPane == null) {
                if (retryIfMissing) Platform.runLater(() -> attachOrUpdateSlidingOverlay(combo, dur, false));
                return;
            }

            final String OVERLAY_ID = "hf-combo-slide-overlay";
            Rectangle bar = (Rectangle) hostPane.lookup('#' + OVERLAY_ID);
            if (bar == null) {
                bar = new Rectangle();
                bar.setId(OVERLAY_ID);
                bar.setManaged(false);
                bar.setArcWidth(12);
                bar.setArcHeight(12);
                bar.setFill(Color.web("#F4FFFF"));
                bar.setStroke(Color.web("#1aa3a8"));
                bar.setStrokeWidth(1);
                bar.setOpacity(0.0);
                hostPane.getChildren().add(bar);
                bar.toBack();
            }

            int sel = combo.getSelectionModel().getSelectedIndex();
            if (sel < 0) return;

            Region targetCell = null;
            for (Node n : lv.lookupAll(".list-cell")) {
                if (n instanceof Region r) {
                    if (r.getStyleClass().contains("selected") || r.isFocused()) {
                        targetCell = r; break;
                    }
                }
            }

            double width = Math.max(0, lv.getWidth());
            double toY, rowH;

            if (targetCell != null && targetCell.getHeight() > 0) {
                rowH = targetCell.getHeight();
                toY  = targetCell.getLayoutY();
            } else {
                rowH = (lv.getFixedCellSize() > 0) ? lv.getFixedCellSize() : 28;
                toY  = sel * rowH; // تقريب لو الخلية مش متحققة
            }

            animateSlidingBar(bar, bar.getY(), toY, width, rowH, dur);
        } catch (Throwable ignore) { }
    }

    private static void animateSlidingBar(Rectangle bar, double fromY, double toY, double width, double height, Duration dur) {
        if (Double.isNaN(fromY)) fromY = toY;
        bar.setWidth(Math.max(0, width));
        bar.setHeight(Math.max(0, height));

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.opacityProperty(), Math.max(0.2, bar.getOpacity())),
                        new KeyValue(bar.yProperty(), fromY, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(dur,
                        new KeyValue(bar.opacityProperty(), 1.0),
                        new KeyValue(bar.yProperty(), toY, Interpolator.EASE_BOTH)
                )
        );
        tl.playFromStart();
    }

    /** خلية مع طبقة تمييز متحركة عند الاختيار والتحويم */
    private static final class AnimatedCell<T> extends ListCell<T> {
        private final Function<T, String> toText;
        private final Duration perItemDelay;
        private final StackPane root = new StackPane();
        private final Label label = new Label();
        private final Rectangle hl = new Rectangle();

        AnimatedCell(Function<T, String> toText, Duration perItemDelay) {
            this.toText = toText;
            this.perItemDelay = perItemDelay == null ? Duration.ZERO : perItemDelay;

            // طبقة التمييز
            hl.setArcWidth(12);
            hl.setArcHeight(12);
            hl.setManaged(false);
            hl.setOpacity(0);
            hl.setFill(Color.web("#F4FFFF")); // خلفية فاتحة متناسقة مع الثيم

            root.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
            root.getChildren().addAll(hl, label);

            // خلّي المستطيل يغطي مساحة الخلية
            root.widthProperty().addListener((o, a, b) -> hl.setWidth(b.doubleValue()));
            root.heightProperty().addListener((o, a, b) -> hl.setHeight(b.doubleValue()));

            setText(null);
            setGraphic(root);

            // حافظ على وضوح لون النص أثناء hover/selected (بدون كسر CSS)
            hoverProperty().addListener((o, was, is) ->
                    label.setTextFill(is ? Color.web("#0E6C70") : Color.BLACK));
            selectedProperty().addListener((o, was, is) -> {
                if (is) label.setTextFill(Color.web("#11878b"));
            });

            // --- Animated hover transition with per-item delay ---
            hoverProperty().addListener((o, was, is) -> {
                if (is) playHoverInAnim(); else playHoverOutAnim();
            });
        }

        @Override protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            setGraphic(root);
            label.setText(toText.apply(item));

            if (isSelected()) {
                playSelectAnim();
            } else {
                hl.setOpacity(0);
                hl.setScaleX(1);
                hl.setTranslateX(0);
            }
        }

        private void playHoverInAnim() {
            hl.setTranslateX(-10);
            hl.setOpacity(0);

            Runnable anim = () -> {
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(hl.opacityProperty(), 0.0),
                                new KeyValue(hl.translateXProperty(), -10, Interpolator.EASE_BOTH)
                        ),
                        new KeyFrame(Duration.millis(160),
                                new KeyValue(hl.opacityProperty(), 1.0)
                        ),
                        new KeyFrame(Duration.millis(220),
                                new KeyValue(hl.translateXProperty(), 0, Interpolator.EASE_OUT)
                        )
                );
                tl.playFromStart();
            };

            if (perItemDelay.greaterThan(Duration.ZERO)) {
                PauseTransition pt = new PauseTransition(perItemDelay);
                pt.setOnFinished(e -> anim.run());
                pt.playFromStart();
            } else {
                anim.run();
            }
        }

        private void playHoverOutAnim() {
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(hl.opacityProperty(), hl.getOpacity())
                    ),
                    new KeyFrame(Duration.millis(120),
                            new KeyValue(hl.opacityProperty(), 0.0, Interpolator.EASE_BOTH)
                    )
            );
            tl.playFromStart();
        }

        private void playSelectAnim() {
            Runnable runAnim = () -> {
                hl.setOpacity(0);
                hl.setScaleX(0.1);
                hl.setTranslateX(-16);
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(hl.opacityProperty(), 0.0),
                                new KeyValue(hl.scaleXProperty(), 0.1, Interpolator.EASE_BOTH),
                                new KeyValue(hl.translateXProperty(), -16, Interpolator.EASE_BOTH)
                        ),
                        new KeyFrame(Duration.millis(150),
                                new KeyValue(hl.opacityProperty(), 1.0)
                        ),
                        new KeyFrame(Duration.millis(220),
                                new KeyValue(hl.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                                new KeyValue(hl.translateXProperty(), 0, Interpolator.EASE_OUT)
                        )
                );
                tl.playFromStart();
            };

            if (perItemDelay.greaterThan(Duration.ZERO)) {
                PauseTransition pt = new PauseTransition(perItemDelay);
                pt.setOnFinished(e -> runAnim.run());
                pt.playFromStart();
            } else {
                runAnim.run();
            }
        }
    }
}