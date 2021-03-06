/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torniquete;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TorniqueteDAO {

    private Connection connection;

    public TorniqueteDAO() {
        connection = Conexion.getConnection();
    }

    public ArrayList consultarSubEventos(int event_id) {
        ArrayList listado = new ArrayList();
        Date date = new Date();
        SimpleDateFormat Formateador = new SimpleDateFormat("yyyy-MM-dd");
        String Fecha = Formateador.format(date);
        SimpleDateFormat FormateadorH = new SimpleDateFormat("HH:mm");
        String Hora = FormateadorH.format(date);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT id, nombre FROM activities WHERE event_id = " + event_id + " AND fecha = '" + Fecha + "' AND '" + Hora + "' BETWEEN hora_inicio AND hora_fin");
            if (rs != null) {
                while (rs.next()) {
                    //ahora tomo los datos de la consulta
                    listado.add(rs.getInt("id"));
                    listado.add(rs.getString("nombre"));
                }
                rs.close();
            } else {
                listado = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return listado;
    }

    public ArrayList consultarEventos() {
        ArrayList listado = new ArrayList();
        Date date = new Date();
        SimpleDateFormat Formateador = new SimpleDateFormat("yyyy-MM-dd");
        String Fecha = Formateador.format(date);
        String sql = "SELECT id, even_nombre FROM events WHERE even_fechInicio <= '" + Fecha + "' AND even_fechFinal >= '" + Fecha + "'";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            System.out.println("SQL "+ sql);
            if (rs != null) {
                while (rs.next()) {
                    //ahora tomo los datos de la consulta
                    listado.add(rs.getInt("id"));
                    listado.add(rs.getString("even_nombre"));
                }
                rs.close();
            } else {
                listado = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("si error");
        }
        return listado;
    }

    /**
     * Esta funcion se encarga de dado el codigo de la tarjeta validar si se
     * puede ingresar
     *
     * @param codigo
     * @return -4 Si la tarjeta no pertenece al evento \n -3 Si la tarjeta no se
     * encuentra en el sistema \n -2 Si la entrada no permite esa categoria de
     * entrada \n -1 Si excedio el limite de entradas \n 0 Si puede entrar
     * normal \n
     */
    public int validarTarjeta(String codigo, String torniquete_id, String event_id) {
        String sql = "select id, categoria_id from inputs where entr_codigo=" + codigo;
        Hashtable<String, String> datos = new Hashtable<String, String>();
        int retornar = -1;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    //ahora tomo los datos de la consulta
                    datos.put("input_id", String.valueOf(rs.getInt("id")));
                    datos.put("categoria_id", String.valueOf(rs.getInt("categoria_id")));
                    System.out.println("cate " + datos.get("categoria_id"));
                    //Determino si esa categoria pertenece a ese evento
                    sql = "select event_id from inputs where id=" + datos.get("input_id");
                    rs.close();
                    ResultSet rs3 = statement.executeQuery(sql);
                    if (rs3 != null) {
                        if (rs3.next()) {
                            datos.put("event_id", String.valueOf(rs3.getInt("event_id")));
                            //Comparo el evento de la categoria con el evento del torniquete
                            if (event_id.equals(datos.get("event_id"))) {
                                //Determino si la entrada puede recibir esa tarjeta
//                                sql = "select id from entradas where category_id=" + datos.get("category_id") + " and id=" + entrada_id;
                                sql = "SELECT e_t.entrada_id, c_e.categoria_id"
                                        + " FROM entradas_torniquetes e_t"
                                        + " INNER JOIN entradas e ON e.id = e_t.entrada_id"
                                        + " INNER JOIN categorias_entradas c_e ON c_e.entrada_id = e.id"
                                        + " WHERE e_t.torniquete_id =" + torniquete_id + ""
                                        + " AND c_e.categoria_id =" + datos.get("categoria_id")
                                        + " AND c_e.event_id = " + datos.get("event_id")
                                        + " LIMIT 0 , 30";
                                System.out.println("sql:" + sql);
                                rs3.close();
                                ResultSet rs2 = statement.executeQuery(sql);
                                if (rs2 != null) {
                                    if (rs2.next()) {
                                        //Tomo los datos de la consulta
                                        datos.put("entrada_id", String.valueOf(rs2.getInt("entrada_id")));
                                        System.out.println("entrada_id: " + datos.get("entrada_id"));
                                        //Determino la cantidad de veces que ha ingresado
                                        rs2.close();
                                        int ingresos = this.cantidadIngresos(datos.get("input_id"), statement);
                                        System.out.println("ingresos: " + ingresos);
                                        if (ingresos >= 0) {
                                            //Valido si cumple las reglas de la tabla validation
                                            int cantidad_reingreso_permitidos = this.getCantidadIngresosByValidation(datos.get("categoria_id"), statement);
                                            System.out.println("cantidad_reingreso_permitidos: " + cantidad_reingreso_permitidos);
                                            if (ingresos < cantidad_reingreso_permitidos || cantidad_reingreso_permitidos == 0) {
                                                //Puede entrar, por ende registro la entrada
                                                this.registrarIngresos(datos.get("input_id"), datos.get("entrada_id"), ingresos, statement);
                                                retornar = 0;
                                                registrarLog(retornar, torniquete_id, datos.get("input_id"), statement);
                                            } else if (cantidad_reingreso_permitidos == -1) {
                                                //No existe regla de validación
                                                retornar = 1;
                                            } else {
                                                //Excedio el limite
                                                retornar = 2;
                                                registrarLog(retornar, torniquete_id, datos.get("input_id"), statement);
                                            }
                                        }
                                    } else {
                                        //Entrada física no habilitada para la categoría
                                        retornar = 3;
                                        registrarLog(retornar, torniquete_id, datos.get("input_id"), statement);
                                    }
                                } else {
                                    //Entrada física no habilitada para la categoría
                                    retornar = 3;
                                    registrarLog(retornar, torniquete_id, datos.get("input_id"), statement);
                                }
                            } else {
                                //El código no es de el evento
                                retornar = 4;
                                registrarLog(retornar, torniquete_id, datos.get("input_id"), statement);
                            }
                        }
                    }
                } else {
                    //Código no registrado
                    retornar = 5;
                    registrarLog(retornar, torniquete_id, "-1", statement);
                }
            } else {
                //Código no registrado
                retornar = 5;
                registrarLog(retornar, torniquete_id, "-1", statement);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Retornar: " + retornar);
        return retornar;
    }

    /**
     * Determina la cantidada de ingresos se han realizado con una tarjeta en
     * especifico
     *
     * @param input_id Id de la entrada que tiene asociada la tarjeta
     * @return Cantidad de ingresos
     */
    public int cantidadIngresos(String input_id, Statement statement) {
        int retornar = -1;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select SUM(ingresos) as cantidad from entradas_inputs where  input_id=" + input_id + " and fecha='" + dateFormat.format(date) + "'";
        System.out.println("Sql: " + sql);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
                if (rs.next()) {
                    retornar = rs.getInt("cantidad");
                }

            } else {
                //Si no hay un registro lo ingreso a la base de datos
                retornar = 0;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        return retornar;
    }

    /**
     * Registra el ingreso en el sistema
     *
     * @param input_id Id del input que tiene asociada la tarjeta
     * @param entrada_id Id del torniquete por donde se ingresa
     * @param ingresos Cantidad de ingresos que ha reali
     * @param statement
     * @return
     */
    public int registrarIngresos(String input_id, String entrada_id, int ingresos, Statement statement) {
        int retornar = -1;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String sql = "";
        if (ingresos > 0) {
            sql = "update entradas_inputs set ingresos=" + (ingresos + 1) + " where entrada_id=" + entrada_id + " and input_id=" + input_id;
        } else {
            sql = "insert into entradas_inputs (entrada_id,input_id,fecha,ingresos) values(" + entrada_id + "," + input_id + ",'" + dateFormat.format(date) + "',1)";
        }
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        return retornar;
    }

    /**
     * Determina la cantidad de ingresos maximo permitidos a una categoria
     * especifica en la fecha actual
     *
     * @param category_id Id de la categoria a consultar
     * @return La cantidad de ingresos maximos
     */
    public int getCantidadIngresosByValidation(String category_id, Statement statement) {
        int retornar = -1;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select cantidad_reingresos from validations where ('" + dateFormat.format(date) + "' BETWEEN fechainicio and fechafin) and categoria_id=" + category_id;
        System.out.println("sql: " + sql);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs != null) {
                if (rs.next()) {
                    //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
                    retornar = rs.getInt("cantidad_reingresos");
                }

            } else {
                //Si no hay un registro lo ingreso a la base de datos
                retornar = -2;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            retornar = -1;
        }
        return retornar;
    }

    public void registrarLog(int tipo, String torniquete_id, String input_id, Statement statement) {
        String descripcion = "";
        String type = "";
        switch (tipo) {
            case 4:
                descripcion = "Tarjeta no pertenece al evento";
                type = "RECHAZO";
                break;
            case 5:
                descripcion = "La tarjeta no se encuentra en el sistema";
                type = "RECHAZO";
                break;
            case 3:
                descripcion = "Entrada no permite esa tarjeta";
                type = "RECHAZO";
                break;
            case 2:
                descripcion = "Se excedio el limite de entradas";
                type = "RECHAZO";
                break;
            case 0:
                descripcion = "";
                type = "INGRESO";
                break;
        }
        String sql = "insert into logs_torniquetes (torniquete_id, input_id, tipo, descripcion) values($1,$2,'$3','$4')";
        sql = sql.replace("$1", torniquete_id);
        sql = sql.replace("$2", input_id);
        sql = sql.replace("$3", type);
        sql = sql.replace("$4", descripcion);
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String obtenerUltimoIngreso(String codigo) {
        String sql = "select l_t.fecha from logs_torniquetes l_t, inputs i where i.entr_codigo=$1 and i.id=l_t.input_id and l_t.tipo='INGRESO' order by l_t.fecha DESC LIMIT 1";
        sql = sql.replace("$1", codigo);
        System.out.println("sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                String fecha = rs.getString("fecha");
                return fecha.substring(0, 16);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String obtenerPersona(String codigo, String event_id) {
        String sql = "select p.pers_primNombre, p.pers_primApellido from people p, inputs i where i.entr_codigo=$1 and i.event_id =$2 and i.person_id=p.id";
        sql = sql.replace("$1", codigo);
        sql = sql.replace("$2", event_id);
        System.out.println("sql: " + sql);
        String nombre = "";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                nombre = rs.getString("pers_primNombre");
//                String apellido = rs.getString("pers_primApellido");
                return nombre;
            } else {
                return nombre;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String consultarUltimoRegistro(String codigo, String torniquete) {
        String sql = "select l.fecha, l.id, CURRENT_TIMESTAMP as fechaActual FROM logs_torniquetes l INNER JOIN inputs i ON i.id=l.input_id AND i.entr_codigo= $1 and l.torniquete_id = $2 ORDER BY id DESC LIMIT 1 ";
        sql = sql.replace("$1", codigo);
        sql = sql.replace("$2", torniquete);
        String respuesta = "false";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            Calendar calFechaInicial = Calendar.getInstance();
            Calendar calFechaFinal = Calendar.getInstance();
            DateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
            if (rs.next()) {
                String Fecha = rs.getString("fecha");
                String Fecha2 = rs.getString("fechaActual");
                calFechaInicial.setTime(df.parse(Fecha));
                calFechaFinal.setTime(df.parse(Fecha2));
//                System.out.println("fecha inicio: " + calFechaInicial + "Fecha fin: " + calFechaFinal);
                long segundos = ((calFechaFinal.getTimeInMillis() - calFechaInicial.getTimeInMillis()) / 1000);
                System.out.println("segundos: " + segundos);
                if (segundos > 2) {
                    respuesta = "false";
                } else {
                    respuesta = "true";
                }
            } else {
                respuesta = "false";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException ex) {
            Logger.getLogger(TorniqueteDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("respuesta: " + respuesta);
        return respuesta;
    }

    public int consultarAforo(int subeven) {
        int aforo = -1;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select aforo from activities where  id =" + subeven + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                aforo = rs.getInt("aforo");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return aforo;
    }

    public int consultarReingresos(int subeven) {
        int aforo = -1;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select reingresos from activities where  id =" + subeven + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                aforo = rs.getInt("reingresos");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return aforo;
    }

    public void insertEntrada(int subeven, int person) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "INSERT INTO activities_people (activity_id, person_id, fecha_entrada) VALUES (" + subeven + "," + person + ",CURRENT_TIMESTAMP)";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            actualizarControlAforo(subeven, 1);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertSalida(int id, int subeven) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "UPDATE activities_people SET fecha_salida = CURRENT_TIMESTAMP WHERE id = " + id + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            actualizarControlAforo(subeven, -1);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void actualizarControlAforo(int subeven, int valor) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "UPDATE activities SET control_aforo = control_aforo +" + valor + " WHERE id = " + subeven + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void actualizarCupo(int cupo, int x) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "UPDATE cupos_activities SET activo = " + x + "  WHERE id = " + cupo + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int consultarIdPersona(String codigo, int evento) {
        int id = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select p.id from people p, inputs i where  i.event_id =" + evento + " AND i.entr_codigo = " + codigo + " AND i.person_id = p.id";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                id = rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return id;
    }

    public int obtenerIngreso(int pers_id, int activity) { // solo para cuando es entrada y salida (aforo >0 )
        int people_activity = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select id from activities_people where  person_id = " + pers_id + " AND activity_id = " + activity + " AND fecha_entrada is not null AND fecha_salida is null";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                people_activity = rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return people_activity;
    }
    
    public int existeIngreso(int pers_id, int activity) { // solo para cuando es entrada y salida (aforo >0 )
        int people_activity = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select id from activities_people where  person_id = " + pers_id + " AND activity_id = " + activity + " AND fecha_entrada is not null AND fecha_salida is not null";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                people_activity = rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return people_activity;
    }

    public int obtenerControlAforo(int activity) { // solo para cuando es entrada y salida (aforo >0 )
        int control_aforo = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select control_aforo from activities where id =" + activity + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                control_aforo = rs.getInt("control_aforo");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return control_aforo;
    }

    public int consultarAlertaAforo(int activity) { // solo para cuando es entrada y salida (aforo >0 )
        int control_aforo = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select alerta_aforo from activities where id =" + activity + "";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                control_aforo = rs.getInt("alerta_aforo");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return control_aforo;
    }

    public int verificarCupo(String codigo,int activity) {
        int cupo = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select ca.id from cupos_activities ca INNER JOIN inputs i ON i.id = ca.input_id where i.entr_codigo =" + codigo + " AND ca.activo = 1 AND ca.activity_id = "+activity+"";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                cupo = rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return cupo;
    }

    public int verificarCupoR(String codigo) {
        int cupo = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select ca.id from cupos_activities ca INNER JOIN inputs i ON i.id = ca.input_id where i.entr_codigo =" + codigo + " ";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                cupo = rs.getInt("id");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return cupo;
    }

    public int consultarCuposAsignados(int activity) {
        int cupo_disp = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql = "select count(*) as total FROM `cupos_activities` WHERE `activity_id`= "+activity+" AND activo = true group by `activity_id`";
        System.out.println("Sql: " + sql);
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite
            if (rs.next()) {
                cupo_disp = rs.getInt("total");
            } else {
                cupo_disp = 0;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return cupo_disp;
    }

    public void insertCupoActivity(int subeven, String codigo) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        System.out.println("Fecha Sistema: " + dateFormat.format(date));
        String sql2 = "SELECT id FROM inputs WHERE entr_codigo = " + codigo + "";
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql2);
            if (rs.next()) {
                int input = rs.getInt("id");
                String sql = "INSERT INTO `cupos_activities`(`activity_id`, `input_id`, `activo`) VALUES (" + subeven + "," + input + ", true)";
                System.out.println("Sql: " + sql);
                statement.execute(sql);
            }
            //Si ya hay un registro en la base de datos, tomo los ingresos y valido si no ha excedido el limite

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
