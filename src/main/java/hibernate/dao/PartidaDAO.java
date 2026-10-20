package hibernate.dao;

import java.util.List;

import hibernate.model.Partida;

public class PartidaDAO extends GenericDAO<Partida, Integer> implements IPartidaDao{

    @Override
    public int IniciarPartida(List<String> nomsJugadors) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'IniciarPartida'");
    }

    @Override
    public void MostrarTauler(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarTauler'");
    }

    @Override
    public void MostrarRecursos(int idJugador) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarRecursos'");
    }

    @Override
    public void MostrarPartida(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'MostrarPartida'");
    }

    @Override
    public void TirarDaus(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'TirarDaus'");
    }

    @Override
    public void ComprovarVictoria(int idPartida) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ComprovarVictoria'");
    }
    
}
