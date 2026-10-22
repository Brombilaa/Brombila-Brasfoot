/*
 * Substitui a classe d.aD do jogo (tabela do editor). A original pintava as linhas
 * pares de creme (#f6f7e5) por cima do tema, deixando o texto claro ilegivel; o
 * zebrado agora fica com o tema do mod.
 */
package d;

import javax.swing.JTable;
import javax.swing.table.TableModel;

class aD
extends JTable {
    final /* synthetic */ n Vh;

    aD(n o, TableModel tableModel) {
        super(tableModel);
        this.Vh = o;
    }
}
