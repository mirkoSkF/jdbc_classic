package spring.crudJdbc.demo.service;

import spring.crudJdbc.demo.model.Dipendente;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CrudService {

    private final String url = "jdbc:mariadb://localhost:3306/dipendenti_hdb";
    private final String user = "root";
    private final String password = "password"; // Inserisci la tua password

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // ====================================================================
    // 1. CREATE - Inserisce un singolo record nella tabella DIPENDENTI
    // ====================================================================
    public void create(Dipendente dipendente) {
        String sql = "INSERT INTO DIPENDENTI (nome, cognome, codice_fiscale, genere, data_di_nascita, luogo_nascita, contatto, titolo_studio, ruolo_aziendale) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dipendente.getNome());
            ps.setString(2, dipendente.getCognome());
            ps.setString(3, dipendente.getCodiceFiscale());
            ps.setString(4, dipendente.getGenere());
            
            if (dipendente.getDataDiNascita() != null) {
                ps.setDate(5, Date.valueOf(dipendente.getDataDiNascita()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            
            ps.setString(6, dipendente.getLuogoNascita()); // Mappa su 'luogo_nascita'
            ps.setString(7, dipendente.getContatto());
            ps.setString(8, dipendente.getTitoloStudio());
            ps.setString(9, dipendente.getRuoloAziendale());

            ps.executeUpdate();
            System.out.println("Dipendente inserito con successo!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ====================================================================
    // 2. READ (ALL) - Recupera tutti i record dalla tabella
    // ====================================================================
    public List<Dipendente> getAll() {
        List<Dipendente> lista = new ArrayList<>();
        String sql = "SELECT * FROM DIPENDENTI";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Dipendente d = new Dipendente();
                d.setId(rs.getLong("id"));
                d.setNome(rs.getString("nome"));
                d.setCognome(rs.getString("cognome"));
                d.setCodiceFiscale(rs.getString("codice_fiscale"));
                d.setGenere(rs.getString("genere"));
                
                Date dataNascita = rs.getDate("data_di_nascita");
                if (dataNascita != null) {
                    d.setDataDiNascita(dataNascita.toLocalDate());
                }
                
                d.setLuogoNascita(rs.getString("luogo_nascita"));
                d.setContatto(rs.getString("contatto"));
                d.setTitoloStudio(rs.getString("titolo_studio"));
                d.setRuoloAziendale(rs.getString("ruolo_aziendale"));

                lista.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ====================================================================
    // 2b. READ (ONE) - Recupera un singolo record tramite ID (usando first())
    // ====================================================================
    public Dipendente getById(long id) {
        String sql = "SELECT * FROM DIPENDENTI WHERE id = ?";
        Dipendente d = null;

        // ATTENZIONE: Dobbiamo esplicitare il tipo di ResultSet SCROLLABLE
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, 
                     ResultSet.TYPE_SCROLL_INSENSITIVE, 
                     ResultSet.CONCUR_READ_ONLY)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                // Posiziona il cursore sulla prima riga assoluta del ResultSet
                if (rs.first()) { 
                    d = new Dipendente();
                    d.setId(rs.getLong("id"));
                    d.setNome(rs.getString("nome"));
                    d.setCognome(rs.getString("cognome"));
                    d.setCodiceFiscale(rs.getString("codice_fiscale"));
                    d.setGenere(rs.getString("genere"));
                    
                    Date dataNascita = rs.getDate("data_di_nascita");
                    if (dataNascita != null) {
                        d.setDataDiNascita(dataNascita.toLocalDate());
                    }
                    
                    d.setLuogoNascita(rs.getString("luogo_nascita"));
                    d.setContatto(rs.getString("contatto"));
                    d.setTitoloStudio(rs.getString("titolo_studio"));
                    d.setRuoloAziendale(rs.getString("ruolo_aziendale"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return d;
    }

    // ====================================================================
    // 3. UPDATE - Aggiorna i dati di un dipendente esistente tramite ID
    // ====================================================================
    public void update(Dipendente dipendente) {
        String sql = "UPDATE DIPENDENTI SET nome=?, cognome=?, codice_fiscale=?, genere=?, data_di_nascita=?, luogo_nascita=?, contatto=?, titolo_studio=?, ruolo_aziendale=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dipendente.getNome());
            ps.setString(2, dipendente.getCognome());
            ps.setString(3, dipendente.getCodiceFiscale());
            ps.setString(4, dipendente.getGenere());
            
            if (dipendente.getDataDiNascita() != null) {
                ps.setDate(5, Date.valueOf(dipendente.getDataDiNascita()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            
            ps.setString(6, dipendente.getLuogoNascita());
            ps.setString(7, dipendente.getContatto());
            ps.setString(8, dipendente.getTitoloStudio());
            ps.setString(9, dipendente.getRuoloAziendale());
            ps.setLong(10, dipendente.getId());

            int righeAggiornate = ps.executeUpdate();
            if (righeAggiornate > 0) {
                System.out.println("Dipendente con ID " + dipendente.getId() + " aggiornato con successo.");
            } else {
                System.out.println("Nessun dipendente trovato con ID: " + dipendente.getId());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ====================================================================
    // 4. DELETE - Rimuove un record tramite il suo ID
    // ====================================================================
    public void delete(long id) {
        String sql = "DELETE FROM DIPENDENTI WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            int righeCancellate = ps.executeUpdate();
            if (righeCancellate > 0) {
                System.out.println("Dipendente rimosso con successo.");
            } else {
                System.out.println("Nessun dipendente trovato con ID: " + id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
