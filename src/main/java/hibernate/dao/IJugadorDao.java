package hibernate.dao;

import hibernate.enums.TipusRecurs;

public interface IJugadorDao {
    void MostrarRecursos(int idJugador);
    boolean ConstruirCarretera(int idJugador, int idAresta);
    boolean ConstruirPoblat(int idJugador, int idVertex);
    boolean MillorarACiutat(int idJugador, int idVertex);
    TipusRecurs ComerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep);
    int CalcularPunts(int idJugador);
    void PassarTorn(int idJugador);
}
