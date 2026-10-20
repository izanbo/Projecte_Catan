package hibernate.dao;

import hibernate.enums.TipusRecurs;
import hibernate.model.Jugador;

public class JugadorDao extends GenericDAO<Jugador, Integer> implements IJugadorDao{

    @Override
    public boolean ConstruirCarretera(int idJugador, int idAresta) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ConstruirCarretera'");
    }

    @Override
    public boolean ConstruirPoblat(int idJugador, int idVertex) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ConstruirPoblat'");
    }

    @Override
    public boolean MillorarACiutat(int idJugador, int idVertex) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MillorarACiutat'");
    }

    @Override
    public TipusRecurs ComerciarBanca(int idJugador, TipusRecurs dona, TipusRecurs rep) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ComerciarBanca'");
    }

    @Override
    public int CalcularPunts(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'CalcularPunts'");
    }

    @Override
    public void PassarTorn(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'PassarTorn'");
    }

}
