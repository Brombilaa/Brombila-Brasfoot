package mod;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

// Desenho do controle de faixa (Forca e Idade na Procura), usado pela classe do jogo
// components.RangeSliderUI reescrita em mod/src-jogo: trilho fino, trecho escolhido em
// dourado e bolinhas iguais as do Editar Jogador (Editor.Trilho).
public final class Faixa {
    private Faixa() {}

    public static void trilho(Graphics g0, Rectangle t, Rectangle de, Rectangle ate, boolean ativo) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float cy = t.y + t.height / 2f;
        g.setColor(new Color(244, 241, 234, 40));
        g.fill(new RoundRectangle2D.Float(t.x, cy - 2, t.width, 4, 4, 4));
        float x1 = de.x + de.width / 2f, x2 = ate.x + ate.width / 2f;
        g.setColor(ativo ? Estreia.OURO : Estreia.APAGADO);
        g.fill(new RoundRectangle2D.Float(Math.min(x1, x2), cy - 2, Math.abs(x2 - x1), 4, 4, 4));
        g.dispose();
    }

    public static void bolinha(Graphics g0, Rectangle r, boolean cima, boolean ativo) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float cx = r.x + r.width / 2f, cy = r.y + r.height / 2f;
        Color ouro = ativo ? Estreia.OURO : Estreia.APAGADO;
        g.setColor(Tema.alfa(ouro, 50));
        g.fill(new Ellipse2D.Float(cx - 9, cy - 9, 18, 18));
        g.setColor(ouro);
        g.fill(new Ellipse2D.Float(cx - 6.5f, cy - 6.5f, 13, 13));
        g.setColor(Estreia.OURO_ESCURO);
        g.fill(new Ellipse2D.Float(cx - 2, cy - 2, 4, 4));
        g.dispose();
    }
}
