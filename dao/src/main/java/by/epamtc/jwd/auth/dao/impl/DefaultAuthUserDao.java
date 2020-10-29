package by.epamtc.jwd.auth.dao.impl;

import by.epamtc.jwd.auth.dao.AuthUserDao;
import by.epamtc.jwd.auth.dao.exception.DaoException;
import by.epamtc.jwd.auth.dao.pool.ConnectionPool;
import by.epamtc.jwd.auth.dao.pool.exception.ConnectionPoolException;
import by.epamtc.jwd.auth.model.auth_info.AuthUser;
import by.epamtc.jwd.auth.model.auth_info.AuthenticationInfo;
import by.epamtc.jwd.auth.model.auth_info.RegistrationInfo;
import by.epamtc.jwd.auth.model.auth_info.Role;
import by.epamtc.jwd.auth.model.constant.AppConstant;
import by.epamtc.jwd.auth.model.constant.SqlStatement;
import org.mindrot.jbcrypt.BCrypt;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DefaultAuthUserDao implements AuthUserDao {
    private ConnectionPool pool = ConnectionPool.getInstance();

    @Override
    public AuthUser receiveAuthUserIfCorrect(AuthenticationInfo authInfo)
            throws DaoException {
        Connection conn = null;
        PreparedStatement selectAuthUserFromDataBaseQuery = null;
        ResultSet rSet = null;

        try {
            conn = pool.takeConnection();
            selectAuthUserFromDataBaseQuery = conn
                    .prepareStatement(SqlStatement.SELECT_AUTH_USER_BY_LOGIN);
            selectAuthUserFromDataBaseQuery.setString(1, authInfo.getLogin());
            rSet = selectAuthUserFromDataBaseQuery.executeQuery();
            if (rSet.next()) {
                String dbPassword = rSet.getString(8);
                if (isPasswordCorrect(authInfo.getPassword(), dbPassword)) {
                    return compileAuthUser(rSet);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("An error while fetching data from DB " +
                    "(auth_user)", e);
        } catch (ConnectionPoolException e) {
            throw new DaoException("An error while taking a connection from " +
                    "the connection pool during logination", e);
        } finally {
            pool.closeConnection(conn, selectAuthUserFromDataBaseQuery, rSet);
        }

        return null;
    }

    @Override
    public AuthUser saveAuthUser(RegistrationInfo regInfo) throws DaoException {
        Connection conn = null;
        PreparedStatement[] statements = new PreparedStatement[3];
        ResultSet rSet = null;
        AuthUser authUser = null;
        int statIndex = 0;

        try {
            conn = pool.takeConnection();
            conn.setAutoCommit(false);

            statements[statIndex] = preparePersonInsertion(regInfo, conn);
            statements[statIndex].executeUpdate();
            int personId = receiveGeneratedKeyAfterStatementExecution
                    (statements[statIndex++]);

            statements[statIndex] = prepareAuthUserInsertion(regInfo, conn,
                    personId);
            statements[statIndex++].executeUpdate();

            statements[statIndex] = prepareAuthUserSelectionWhileRegistration
                    (regInfo, conn);
            rSet = statements[statIndex].executeQuery();
            if (rSet.next()) {
                authUser = compileAuthUser(rSet);
            }

            conn.commit();
        } catch (SQLException e) {
            pool.rollBackTransaction(conn);
            throw new DaoException("An error while saving AuthUser", e);
        } catch (ConnectionPoolException e) {
            throw new DaoException("An error while taking a connection from " +
                    "connection pool during saving AuthUser", e);
        } finally {
            pool.closeConnection(conn, statements, rSet);
        }

        if (authUser == null) {
            throw new DaoException(String.format("AuthUser saving met an " +
                    "impossible execution outcome: registration information: " +
                    "\"%s\"", regInfo.getPassword()));
        }

        return authUser;
    }

    @Override
    public boolean containsLogin(String login) throws DaoException {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = pool.takeConnection();
            stat = conn.prepareStatement("SELECT login FROM " +
                    "hospital.auth_user WHERE login = ?");
            stat.setString(1, login);
            return stat.executeQuery().next();
        } catch (SQLException e) {
            throw new DaoException("An error while fetching login from " +
                    "DB(auth_user", e);
        } catch (ConnectionPoolException e) {
            throw new DaoException("An error while taking a connection from " +
                    "the connection pool during login verification", e);
        } finally {
            pool.closeConnection(conn, stat);
        }
    }

    @Override
    public boolean containsEmail(String email) throws DaoException {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = pool.takeConnection();
            stat = conn.prepareStatement("SELECT email FROM " +
                    "hospital.persons WHERE email = ?");
            stat.setString(1, email);
            return stat.executeQuery().next();
        } catch (SQLException e) {
            throw new DaoException("An error while fetching email from " +
                    "DB(auth_user", e);
        } catch (ConnectionPoolException e) {
            throw new DaoException("An error while taking a connection from " +
                    "the connection pool during email verification", e);
        } finally {
            pool.closeConnection(conn, stat);
        }
    }

    private boolean isPasswordCorrect(String plainPassword, String dbPassword) {
        return BCrypt.checkpw(plainPassword, dbPassword);
    }

    private AuthUser compileAuthUser(ResultSet resultSet) throws SQLException {
        int authId = resultSet.getInt(1);
        String firstName = resultSet.getString(2);
        String middleName = resultSet.getString(3);
        String lastName = resultSet.getString(4);
        Role role = Role.valueOf(resultSet.getString(5));
        int userId = resultSet.getInt(6);
        int staffId = resultSet.getInt(7);
        return new AuthUser(authId, firstName, middleName, lastName,
                role, userId, staffId);
    }

    private PreparedStatement preparePersonInsertion(RegistrationInfo
            registrationInfo, Connection conn) throws SQLException {
        PreparedStatement personCreationStat;
        personCreationStat = conn.prepareStatement(
                SqlStatement.INSERT_PERSON_WHILE_REGISTRATION,
                Statement.RETURN_GENERATED_KEYS);
        personCreationStat.setString(1, registrationInfo.getEmail());
        personCreationStat.setString(2, registrationInfo.getPhoneNumber());
        personCreationStat.setString(3, registrationInfo.getFirstName());
        personCreationStat.setString(4, registrationInfo.getMiddleName());
        personCreationStat.setString(5, registrationInfo.getLastName());
        personCreationStat.setObject(6, registrationInfo.getBirthday());
        personCreationStat.setString(7, registrationInfo.getGender()
                .getGenderName());
        return personCreationStat;
    }

    private PreparedStatement prepareAuthUserInsertion(RegistrationInfo
            registrationInfo, Connection conn, int personId)
            throws SQLException {
        PreparedStatement authUserCreationStat;
        authUserCreationStat = conn.prepareStatement(
                SqlStatement.INSERT_AUTH_USER_WHILE_REGISTRATION,
                Statement.RETURN_GENERATED_KEYS);
        authUserCreationStat.setString(1, registrationInfo.getLogin());
        authUserCreationStat.setString(2, registrationInfo.getPassword());
        authUserCreationStat.setInt(3, Role.USER.getRoleId());
        authUserCreationStat.setInt(4, personId);
        return authUserCreationStat;
    }

    private PreparedStatement prepareAuthUserSelectionWhileRegistration
            (RegistrationInfo regInfo, Connection conn) throws SQLException {
        PreparedStatement selectAuthUser;
        selectAuthUser = conn.prepareStatement(SqlStatement
                .SELECT_AUTH_USER_BY_LOGIN);
        selectAuthUser.setString(1, regInfo.getLogin());
        return selectAuthUser;
    }

    private int receiveGeneratedKeyAfterStatementExecution(PreparedStatement
            personCreationStat) throws SQLException {
        int personId;
        personId = AppConstant.AUTH_USER_STANDARD_INT_VALUE;
        ResultSet personsGeneratedKeys = personCreationStat.getGeneratedKeys();
        while (personsGeneratedKeys.next()) {
            personId = personsGeneratedKeys.getInt(1);
        }
        return personId;
    }
}
