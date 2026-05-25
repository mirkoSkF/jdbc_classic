package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import connettore.Connettore;
import model.Categoria;
import model.Marca;
import model.Ordine;
import model.Prodotto;

public class CrudService implements ICrudService {

	@Override
	public boolean inserisci(Prodotto prodotto) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		String comandoSQL = "insert into prodotti (nome_prodotto,prezzo,quantita,id_marca,id_categoria) values (?,?,?,?,?)";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setString(1, prodotto.getNomeProdotto());
			ps.setDouble(2, prodotto.getPrezzo());
			ps.setInt(3, prodotto.getQuantita());
			ps.setInt(4, prodotto.getMarca().getId());
			ps.setInt(5, prodotto.getCategoria().getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				conn.close(); //chiusura effettiva delle connessione
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	//Read All
	@Override
	public List<Prodotto> leggi() {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Prodotto> prodotti = new ArrayList<Prodotto>();
		Prodotto prodotto = null;
		String comandoSQL = "select * from prodotti order by id";
		try {
			ps = conn.prepareStatement(comandoSQL);
			rs = ps.executeQuery();
			while(rs.next()) { //next() iteratore
				prodotto = new Prodotto();
				prodotto.setId(rs.getInt("id"));
				prodotto.setNomeProdotto(rs.getString("nome_prodotto"));
				prodotto.setPrezzo(rs.getDouble("prezzo"));
				prodotto.setQuantita(rs.getInt("quantita"));
				prodotto.setMarca(leggiMarca(rs.getInt("id_marca")));
				prodotto.setCategoria(leggiCategoria(rs.getInt("id_categoria")));
				prodotti.add(prodotto);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return prodotti;
	}

	//Read One
	@Override
	public Prodotto leggi(int id) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Prodotto prodotto = new Prodotto();
		String comandoSQL = "select * from prodotti where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				prodotto.setId(rs.getInt("id"));
				prodotto.setNomeProdotto(rs.getString("nome_prodotto"));
				prodotto.setPrezzo(rs.getDouble("prezzo"));
				prodotto.setQuantita(rs.getInt("quantita"));
				prodotto.setMarca(leggiMarca(rs.getInt("id_marca")));
				prodotto.setCategoria(leggiCategoria(rs.getInt("id_categoria")));
				return prodotto;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean modifica(Prodotto prodotto) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		String comandoSQL = "update prodotti set nome_prodotto=?,prezzo=?,quantita=?,id_marca=?,id_categoria=? where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setString(1, prodotto.getNomeProdotto());
			ps.setDouble(2, prodotto.getPrezzo());
			ps.setInt(3,prodotto.getQuantita());
			ps.setInt(4, prodotto.getMarca().getId());
			ps.setInt(5, prodotto.getCategoria().getId());
			ps.setInt(6,prodotto.getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean rimuovi(int id) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		String comandoSQL = "delete from prodotti where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, id);
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public Marca leggiMarca(int id) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Marca marca = new Marca();
		String comandoSQL = "select * from marche where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				marca.setId(rs.getInt("id"));
				marca.setNomeMarca(rs.getString("nome_marca"));
				return marca;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public Categoria leggiCategoria(int id) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Categoria categoria = new Categoria();
		String comandoSQL = "select * from categorie where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				categoria.setId(rs.getInt("id"));
				categoria.setNomeCategoria(rs.getString("nome_categoria"));
				return categoria;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public Ordine leggiOrdine(int id) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Ordine ordine = new Ordine();
		String comandoSQL = "select * from ordini where id=?";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				ordine.setId(rs.getInt("id"));
				ordine.setSerialeOrdine(rs.getString("seriale_ordine"));
				return ordine;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean accoppia(Prodotto prodotto, Ordine ordine) {
		Connettore connettore = new Connettore();
		Connection conn = connettore.apriConnessione();
		PreparedStatement ps = null;
		String comandoSQL = "insert into ordini_prodotti (id_prodotto,id_ordine) values (?,?)";
		try {
			ps = conn.prepareStatement(comandoSQL);
			ps.setInt(1, prodotto.getId());
			ps.setInt(2, ordine.getId());
			ps.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				conn.close(); //chiusura effettiva delle connessione
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

}
