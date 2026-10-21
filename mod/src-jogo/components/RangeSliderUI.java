// Classe do jogo (components.RangeSliderUI, controle de Forca e Idade da Procura) reescrita
// pelo mod: mesma logica do original, mas o trilho e as bolinhas sao desenhados por
// mod.Faixa no estilo dourado do Editar Jogador.
package components;

import best.aq;
import c.a;
import components.aa;
import components.ab;
import components.ac;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

class RangeSliderUI
extends BasicSliderUI {
    private Color QY = aq.E(a.vM().getCorTema(), 3);
    private Rectangle QZ;
    private boolean Ra;
    private transient boolean Rb;
    private transient boolean Rc;

    public RangeSliderUI(aa aa2) {
        super(aa2);
    }

    @Override
    public void installUI(JComponent jComponent) {
        this.QZ = new Rectangle();
        super.installUI(jComponent);
    }

    @Override
    protected BasicSliderUI.TrackListener createTrackListener(JSlider jSlider) {
        return new ac(this);
    }

    @Override
    protected ChangeListener createChangeListener(JSlider jSlider) {
        return new ab(this);
    }

    @Override
    protected void calculateThumbSize() {
        super.calculateThumbSize();
        this.QZ.setSize(this.thumbRect.width, this.thumbRect.height);
    }

    @Override
    protected void calculateThumbLocation() {
        int n2;
        super.calculateThumbLocation();
        if (this.slider.getSnapToTicks()) {
            int n3 = n2 = this.slider.getValue() + this.slider.getExtent();
            int n4 = this.slider.getMajorTickSpacing();
            int n5 = this.slider.getMinorTickSpacing();
            int n6 = 0;
            if (n5 > 0) {
                n6 = n5;
            } else if (n4 > 0) {
                n6 = n4;
            }
            if (n6 != 0) {
                if ((n2 - this.slider.getMinimum()) % n6 != 0) {
                    float f2 = (float)(n2 - this.slider.getMinimum()) / (float)n6;
                    int n7 = Math.round(f2);
                    n3 = this.slider.getMinimum() + n7 * n6;
                }
                if (n3 != n2) {
                    this.slider.setExtent(n3 - this.slider.getValue());
                }
            }
        }
        if (this.slider.getOrientation() == 0) {
            n2 = this.xPositionForValue(this.slider.getValue() + this.slider.getExtent());
            this.QZ.x = n2 - this.QZ.width / 2;
            this.QZ.y = this.trackRect.y;
        } else {
            n2 = this.yPositionForValue(this.slider.getValue() + this.slider.getExtent());
            this.QZ.x = this.trackRect.x;
            this.QZ.y = n2 - this.QZ.height / 2;
        }
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(18, 18);
    }

    @Override
    public void paint(Graphics graphics, JComponent jComponent) {
        super.paint(graphics, jComponent);
        Rectangle rectangle = graphics.getClipBounds();
        if (this.Ra) {
            if (rectangle.intersects(this.thumbRect)) {
                this.a(graphics);
            }
            if (rectangle.intersects(this.QZ)) {
                this.b(graphics);
            }
        } else {
            if (rectangle.intersects(this.QZ)) {
                this.b(graphics);
            }
            if (rectangle.intersects(this.thumbRect)) {
                this.a(graphics);
            }
        }
    }

    @Override
    public void paintTrack(Graphics graphics) {
        mod.Faixa.trilho(graphics, this.trackRect, this.thumbRect, this.QZ, this.slider.isEnabled());
    }

    @Override
    public void paintFocus(Graphics graphics) {
    }

    @Override
    public void paintThumb(Graphics graphics) {
    }

    private void a(Graphics graphics) {
        mod.Faixa.bolinha(graphics, this.thumbRect, this.Ra, this.slider.isEnabled());
    }

    private void b(Graphics graphics) {
        mod.Faixa.bolinha(graphics, this.QZ, !this.Ra, this.slider.isEnabled());
    }

    private Shape P(int n2, int n3) {
        Ellipse2D.Double double_ = new Ellipse2D.Double(0.0, 0.0, n2, n3);
        return double_;
    }

    private void Q(int n2, int n3) {
        Rectangle rectangle = new Rectangle();
        rectangle.setBounds(this.QZ);
        this.QZ.setLocation(n2, n3);
        SwingUtilities.computeUnion(this.QZ.x, this.QZ.y, this.QZ.width, this.QZ.height, rectangle);
        this.slider.repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void scrollByBlock(int n2) {
        JSlider jSlider = this.slider;
        synchronized (jSlider) {
            int n3 = (this.slider.getMaximum() - this.slider.getMinimum()) / 10;
            if (n3 <= 0 && this.slider.getMaximum() > this.slider.getMinimum()) {
                n3 = 1;
            }
            int n4 = n3 * (n2 > 0 ? 1 : -1);
            if (this.Ra) {
                int n5 = ((aa)this.slider).uv();
                ((aa)this.slider).dP(n5 + n4);
            } else {
                int n6 = this.slider.getValue();
                this.slider.setValue(n6 + n4);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void scrollByUnit(int n2) {
        JSlider jSlider = this.slider;
        synchronized (jSlider) {
            int n3 = 1 * (n2 > 0 ? 1 : -1);
            if (this.Ra) {
                int n4 = ((aa)this.slider).uv();
                ((aa)this.slider).dP(n4 + n3);
            } else {
                int n5 = this.slider.getValue();
                this.slider.setValue(n5 + n3);
            }
        }
    }

    static /* synthetic */ boolean a(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.Rb;
    }

    static /* synthetic */ boolean b(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.Rc;
    }

    static /* synthetic */ JSlider c(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.slider;
    }

    static /* synthetic */ boolean d(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.Ra;
    }

    static /* synthetic */ Rectangle e(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.QZ;
    }

    static /* synthetic */ Rectangle f(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.thumbRect;
    }

    static /* synthetic */ void a(RangeSliderUI rangeSliderUI, boolean bl2) {
        rangeSliderUI.Ra = bl2;
    }

    static /* synthetic */ void b(RangeSliderUI rangeSliderUI, boolean bl2) {
        rangeSliderUI.Rb = bl2;
    }

    static /* synthetic */ void c(RangeSliderUI rangeSliderUI, boolean bl2) {
        rangeSliderUI.Rc = bl2;
    }

    static /* synthetic */ Rectangle g(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.trackRect;
    }

    static /* synthetic */ int a(RangeSliderUI rangeSliderUI, int n2) {
        return rangeSliderUI.yPositionForValue(n2);
    }

    static /* synthetic */ boolean h(RangeSliderUI rangeSliderUI) {
        return rangeSliderUI.drawInverted();
    }

    static /* synthetic */ int b(RangeSliderUI rangeSliderUI, int n2) {
        return rangeSliderUI.xPositionForValue(n2);
    }

    static /* synthetic */ void a(RangeSliderUI rangeSliderUI, int n2, int n3) {
        rangeSliderUI.Q(n2, n3);
    }
}

