package spring.crudJdbc.demo.service;

import spring.crudJdbc.demo.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CrudService {

    // URL, user e password del tuo database MariaDB
    private final String url = "jdbc:mariadb://localhost:3306/dipendenti_hdb";
    private final String user = "root";
    private final String password = "password"; // Inserisci la tua password

    // Metodo di utilità per ottenere la connessione al database
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // ====================================================================
    // 1. CREATE - Salva Dipendente, Account, Contatti e Relazioni ManyToMany
    // ====================================================================
    public void create(Dipendente dipendente) {
        String sqlDipendente = "INSERT INTO dipendenti (nome, cognome, codice_fiscale, genere, data_di_nascita, luogo_nascita, ruolo_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlAccount = "INSERT INTO accounts (username, password, dipendente_id) VALUES (?, ?, ?)";
        String sqlContatto = "INSERT INTO contatti (valore, tipo_contatto_id, dipendente_id) VALUES (?, ?, ?)";
        String sqlTitolo = "INSERT INTO dipendenti_titoli (dipendente_id, titolo_studio_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            // Disabilitiamo l'autocommit per gestire tutto in un'unica transazione atomica
            conn.setAutoCommit(false);

            long dipendenteId = -1;

            // 1a. Inserimento del Dipendente (Recuperiamo l'ID generato dall'auto_increment)
            try (PreparedStatement psDip = conn.prepareStatement(sqlDipendente, Statement.RETURN_GENERATED_KEYS)) {
                psDip.setString(1, dipendente.getNome());
                psDip.setString(2, dipendente.getCognome());
                psDip.setString(3, dipendente.getCodiceFiscale());
                psDip.setString(4, dipendente.getGenere());
                psDip.setDate(5, Date.valueOf(dipendente.getDataDiNascita()));
                psDip.setString(6, dipendente.getLuogoNascita());
                
                if (dipendente.getRuolo() != null) {
                    psDip.setLong(7, dipendente.getRuolo().getId());
                } else {
                    psDip.setNull(7, Types.BIGINT);
                }

                psDip.executeUpdate();

                try (ResultSet generatedKeys = psDip.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        dipendenteId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creazione dipendente fallita, nessun ID generato.");
                    }
                }
            }

            // 1b. Inserimento dell'Account (Relazione 1:1)
            if (dipendente.getAccount() != null) {
                try (PreparedStatement psAcc = conn.prepareStatement(sqlAccount)) {
                    psAcc.setString(1, dipendente.getAccount().getUsername());
                    psAcc.setString(2, dipendente.getAccount().getPassword());
                    psAcc.setLong(3, dipendenteId);
                    psAcc.executeUpdate();
                }
            }

            // 1c. Inserimento dei Contatti (Relazione 1:N)
            if (dipendente.getContatti() != null && !dipendente.getContatti().isEmpty()) {
                try (PreparedStatement psCont = conn.prepareStatement(sqlContatto)) {
                    for (Contatto contatto : dipendente.getContatti()) {
                        psCont.setString(1, contatto.getValore());
                        psCont.setLong(2, contatto.getTipoContatto().getId()); // Assume TipoContatto non nullo
                        psCont.setLong(3, dipendenteId);
                        psCont.addBatch(); // Usiamo il batch per ottimizzare le performance
                    }
                    psCont.executeBatch();
                }
            }

            // 1d. Inserimento dei Titoli di Studio nella tabella pivot (Relazione M:N)
            if (dipendente.getTitoliStudio() != null && !dipendente.getTitoliStudio().isEmpty()) {
                try (PreparedStatement psTit = conn.prepareStatement(sqlTitolo)) {
                    for (TitoloStudio titolo : dipendente.getTitoliStudio()) {
                        psTit.setLong(1, dipendenteId);
                        psTit.setLong(2, titolo.getId());
                        psTit.addBatch();
                    }
                    psTit.executeBatch();
                }
            }

            // Se tutto è andato a buon fine, salviamo i dati definitivamente
            conn.commit();
            System.out.println("Dipendente e relazioni salvati con successo. ID: " + dipendenteId);

        } catch (SQLException e) {
            // Se qualcosa fallisce, facciamo il rollback completo
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Errore riscontrato. Rollback eseguito.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ====================================================================
    // 2. READ (ALL) - Estrae tutti i dipendenti compilando le relazioni base
    // ====================================================================
    public List<Dipendente> getAll() {
        List<Dipendente> lista = new ArrayList<>();
        // Query con JOIN per ottimizzare il recupero di Dipendente, Ruolo e Account
        String sql = "SELECT d.*, r.denominazione AS ruolo_desc, a.id AS acc_id, a.username, a.password " +
                     "FROM dipendenti d " +
                     "LEFT JOIN ruoli_aziendali r ON d.ruolo_id = r.id " +
                     "LEFT JOIN accounts a ON d.id = a.dipendente_id";

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

                // Mappa il Ruolo
                long ruoloId = rs.getLong("ruolo_id");
                if (!rs.wasNull()) {
                    RuoloAziendale r = new RuoloAziendale();
                    r.setId(ruoloId);
                    r.setDenominazione(rs.getString("ruolo_desc"));
                    d.setRuolo(r);
                }

                // Mappa l'Account
                long accId = rs.getLong("acc_id");
                if (!rs.wasNull()) {
                    Account acc = new Account();
                    acc.setId(accId);
                    acc.setUsername(rs.getString("username"));
                    acc.setPassword(rs.getString("password"));
                    d.setAccount(acc);
                }

                // NOTA DIDATTICA: Per i contatti (1:N) e i titoli (M:N) andrebbero fatte 
                // delle query secondarie filtrate per l'ID del dipendente per popolare le liste.
                lista.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ====================================================================
    // 2b. READ (ONE) - Recupera un singolo dipendente completando tutte le relazioni
    // ====================================================================
    public Dipendente getById(long id) {
        String sqlDipendente = "SELECT d.*, r.denominazione AS ruolo_desc, a.id AS acc_id, a.username, a.password " +
                               "FROM dipendenti d " +
                               "LEFT JOIN ruoli_aziendali r ON d.ruolo_id = r.id " +
                               "LEFT JOIN accounts a ON d.id = a.dipendente_id " +
                               "WHERE d.id = ?";
        
        String sqlContatti = "SELECT c.*, tc.denominazione AS tipo_desc " +
                             "FROM contatti c " +
                             "LEFT JOIN tipi_contatto tc ON c.tipo_contatto_id = tc.id " +
                             "WHERE c.dipendente_id = ?";
        
        String sqlTitoli = "SELECT ts.* FROM titoli_studio ts " +
                           "JOIN dipendenti_titoli dt ON ts.id = dt.titolo_studio_id " +
                           "WHERE dt.dipendente_id = ?";

        Dipendente d = null;

        try (Connection conn = getConnection()) {
            
            // 1. Recupero l'anagrafica base, il Ruolo e l'Account
            try (PreparedStatement psDip = conn.prepareStatement(sqlDipendente)) {
                psDip.setLong(1, id);
                try (ResultSet rs = psDip.executeQuery()) {
                    if (rs.next()) {
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

                        // Mappa il Ruolo (N:1)
                        long ruoloId = rs.getLong("ruolo_id");
                        if (!rs.wasNull()) {
                            RuoloAziendale r = new RuoloAziendale();
                            r.setId(ruoloId);
                            r.setDenominazione(rs.getString("ruolo_desc"));
                            d.setRuolo(r);
                        }

                        // Mappa l'Account (1:1)
                        long accId = rs.getLong("acc_id");
                        if (!rs.wasNull()) {
                            Account acc = new Account();
                            acc.setId(accId);
                            acc.setUsername(rs.getString("username"));
                            acc.setPassword(rs.getString("password"));
                            d.setAccount(acc); // Il setter interno imposta già la bidirezionalità
                        }
                    }
                }
            }

            // Se il dipendente esiste, vado a recuperare le sue liste collegate
            if (d != null) {
                
                // 2. Recupero i Contatti (1:N)
                try (PreparedStatement psCont = conn.prepareStatement(sqlContatti)) {
                    psCont.setLong(1, id);
                    try (ResultSet rsCont = psCont.executeQuery()) {
                        while (rsCont.next()) {
                            Contatto contatto = new Contatto();
                            contatto.setId(rsCont.getLong("id"));
                            contatto.setValore(rsCont.getString("valore"));
                            
                            // Popola il tipo di contatto associato
                            long tipoId = rsCont.getLong("tipo_contatto_id");
                            if (!rsCont.wasNull()) {
                                TipoContatto tc = new TipoContatto();
                                tc.setId(tipoId);
                                tc.setDenominazione(rsCont.getString("tipo_desc"));
                                contatto.setTipoContatto(tc);
                            }
                            
                            d.getContatti().add(contatto);
                        }
                    }
                }

                // 3. Recupero i Titoli di Studio attraverso la tabella pivot (M:N)
                try (PreparedStatement psTit = conn.prepareStatement(sqlTitoli)) {
                    psTit.setLong(1, id);
                    try (ResultSet rsTit = psTit.executeQuery()) {
                        while (rsTit.next()) {
                            TitoloStudio titolo = new TitoloStudio();
                            titolo.setId(rsTit.getLong("id"));
                            titolo.setDenominazione(rsTit.getString("denominazione")); // Assume colonna denominazione o simile
                            
                            d.getTitoliStudio().add(titolo);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return d;
    }

    // ====================================================================
    // 3. UPDATE - Aggiorna i dati anagrafici e riallinea le tabelle collegate
    // ====================================================================
    public void update(Dipendente dipendente) {
        String sqlUpdateDip = "UPDATE dipendenti SET nome=?, cognome=?, codice_fiscale=?, genere=?, data_di_nascita=?, luogo_nascita=?, ruolo_id=? WHERE id=?";
        String sqlDeleteTitoli = "DELETE FROM dipendenti_titoli WHERE dipendente_id=?";
        String sqlInsertTitolo = "INSERT INTO dipendenti_titoli (dipendente_id, titolo_studio_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 3a. Aggiorna anagrafica principale
            try (PreparedStatement psDip = conn.prepareStatement(sqlUpdateDip)) {
                psDip.setString(1, dipendente.getNome());
                psDip.setString(2, dipendente.getCognome());
                psDip.setString(3, dipendente.getCodiceFiscale());
                psDip.setString(4, dipendente.getGenere());
                psDip.setDate(5, Date.valueOf(dipendente.getDataDiNascita()));
                psDip.setString(6, dipendente.getLuogoNascita());
                
                if (dipendente.getRuolo() != null) {
                    psDip.setLong(7, dipendente.getRuolo().getId());
                } else {
                    psDip.setNull(7, Types.BIGINT);
                }
                psDip.setLong(8, dipendente.getId());
                psDip.executeUpdate();
            }

            // 3b. Gestione della relazione ManyToMany (approccio classico: cancella i vecchi legami e inserisce i nuovi)
            try (PreparedStatement psDelTit = conn.prepareStatement(sqlDeleteTitoli)) {
                psDelTit.setLong(1, dipendente.getId());
                psDelTit.executeUpdate();
            }

            if (dipendente.getTitoliStudio() != null && !dipendente.getTitoliStudio().isEmpty()) {
                try (PreparedStatement psInsTit = conn.prepareStatement(sqlInsertTitolo)) {
                    for (TitoloStudio titolo : dipendente.getTitoliStudio()) {
                        psInsTit.setLong(1, dipendente.getId());
                        psInsTit.setLong(2, titolo.getId());
                        psInsTit.addBatch();
                    }
                    psInsTit.executeBatch();
                }
            }

            conn.commit();
            System.out.println("Dipendente aggiornato con successo. ID: " + dipendente.getId());

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ====================================================================
    // 4. DELETE - Elimina rispettando l'ordine delle FK per evitare vincoli bloccanti
    // ====================================================================
    public void delete(long dipendenteId) {
        // Dobbiamo cancellare prima i figli e le tabelle di giunzione, altrimenti il database lancia l'errore d'integrità
        String sqlDeleteTitoli = "DELETE FROM dipendenti_titoli WHERE dipendente_id=?";
        String sqlDeleteContatti = "DELETE FROM contatti WHERE dipendente_id=?";
        String sqlDeleteAccount = "DELETE FROM accounts WHERE dipendente_id=?";
        String sqlDeleteDip = "DELETE FROM dipendenti WHERE id=?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Elimina i legami dei titoli di studio
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteTitoli)) {
                ps.setLong(1, dipendenteId);
                ps.executeUpdate();
            }

            // 2. Elimina i contatti del dipendente
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteContatti)) {
                ps.setLong(1, dipendenteId);
                ps.executeUpdate();
            }

            // 3. Elimina l'account
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteAccount)) {
                ps.setLong(1, dipendenteId);
                ps.executeUpdate();
            }

            // 4. Infine, possiamo cancellare in sicurezza il dipendente
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteDip)) {
                ps.setLong(1, dipendenteId);
                int righeCancellate = ps.executeUpdate();
                if (righeCancellate == 0) {
                    System.out.println("Nessun dipendente trovato con ID: " + dipendenteId);
                }
            }

            conn.commit();
            System.out.println("Dipendente e tutti i suoi dati correlati eliminati con successo.");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
