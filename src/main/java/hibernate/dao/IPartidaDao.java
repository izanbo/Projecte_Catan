package hibernate.dao;

import java.util.List;


public interface IPartidaDao {
    int IniciarPartida(List<String> nomsJugadors);
    void MostrarTauler(int idPartida);
    void MostrarPartida(int idPartida);
    void TirarDaus(int idPartida);
    void ComprovarVictoria(int idPartida);
}   
