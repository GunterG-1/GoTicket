package com.GoTicket.Catalogo.Evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import com.GoTicket.Catalogo.Evento.Model.Evento;
import com.GoTicket.Catalogo.Evento.Repository.EventoRepository;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CommandLineRunner cargarEventosIniciales(EventoRepository eventoRepository) {
		return args -> eventosIniciales().forEach(evento ->
			eventoRepository.findByNombre(evento.getNombre()).orElseGet(() -> eventoRepository.save(evento)));
	}

	private List<Evento> eventosIniciales() {
		return List.of(
			evento("Maria Becerra - Quimera", "Pop Urbano", "Santander Arena - Santiago Centro", "01 de diciembre 2026", "https://static.ptocdn.net/images/eventos/biz392_calugalistado.jpg", "https://www.puntoticket.com/maria-becerra", 85000, 12000),
			evento("2026-27 Taemin World Tour In Santiago", "Fan Meeting", "Santander Arena - Santiago Centro", "02 de noviembre 2026", "https://static.ptocdn.net/images/eventos/jts133_calugalistado.jpg", "https://www.puntoticket.com/taemin-world-tour", 95000, 10000),
			evento("Fonda Doña Rosa", "Fonda", "Arena Hipódromo - Independencia", "18 de septiembre 2026", "https://static.ptocdn.net/images/eventos/jts125_calugalistado.jpg", "https://www.puntoticket.com/dona-rosa-2026", 25000, 15000),
			evento("Colo-Colo vs Deportes Concepción", "Fútbol", "Estadio Monumental - Macul", "13 de septiembre 2026", "https://static.ptocdn.net/images/eventos/byn290_calugalistado.jpg", "https://www.puntoticket.com/evento/colo-colo-vs-deportes-concepcion-ligaprimera-2026", 18000, 40000),
			evento("VIBRAFEST 2026", "Festival", "Arena Hipódromo - Independencia", "08 de diciembre 2026", "https://static.ptocdn.net/images/eventos/jts129_calugalistado.jpg", "https://www.puntoticket.com/vibra-fest-2026", 45000, 18000),
			evento("Creamfields Chile 2026 - 21 Años", "Electrónica", "Club Hípico - Santiago Centro", "14 de noviembre 2026", "https://static.ptocdn.net/images/eventos/sm0194_calugalistado.jpg", "https://www.puntoticket.com/creamfields-2026", 65000, 25000),
			evento("Macha y El Bloque Depresivo", "Boleros", "Estadio Nacional - Ñuñoa", "11 de diciembre 2026", "https://static.ptocdn.net/images/eventos/ulk006_calugalistado.jpg", "https://www.puntoticket.com/macha-y-el-bloque-depresivo", 35000, 22000),
			evento("Fonda Doña Florinda", "Fonda", "Parque Los Heroes - La Florida", "18 de septiembre 2026", "https://static.ptocdn.net/images/eventos/jts124_calugalistado.jpg", "https://www.puntoticket.com/dona-florinda-2026", 22000, 12000),
			evento("Cirque du Soleil - Alegria", "Familia", "Espacio Riesco - Huechuraba", "06 de enero 2027", "https://static.ptocdn.net/images/eventos/ipr002v4_calugalistado.jpg", "https://www.puntoticket.com/cirque-du-soleil-alegria", 70000, 8000),
			evento("El Show de Alejo Igoa", "Youtuber", "Santander Arena - Santiago Centro", "12 de noviembre 2026", "https://static.ptocdn.net/images/eventos/lot231_calugalistado.jpg", "https://www.puntoticket.com/el-show-de-alejo-igoa-2026", 30000, 9000),
			evento("La Gran Fonda De Aconcagua 2026", "Fonda", "Palacio Hacienda de Quilpué - San Felipe", "17 de septiembre 2026", "https://static.ptocdn.net/images/eventos/ive004_calugalistado.jpg", "https://www.puntoticket.com/la-gran-fonda-aconcagua-2026", 30000, 7000),
			evento("Helloween - 40 Years Anniversary", "Heavy Metal", "Santander Arena - Santiago Centro", "11 de septiembre 2026", "https://static.ptocdn.net/images/eventos/rec107_calugalistado.jpg", "https://www.puntoticket.com/helloween", 55000, 10000),
			evento("Piknic Electronik - Pase De Temporada 2026", "Fiesta", "Parque Fisa - Pudahuel", "01 de octubre 2026", "https://static.ptocdn.net/images/eventos/glo205_calugalistado.jpg", "https://www.puntoticket.com/temporada-piknic-2026", 90000, 20000),
		evento("Def Leppard - Live 2026", "Rock", "Santander Arena - Santiago Centro", "08 de noviembre 2026", "https://static.ptocdn.net/images/eventos/rec113_calugalistado.jpg", "https://www.puntoticket.com/def-leppard", 80000, 12000),
			evento("Piknic Electronik #1", "Fiesta Electrónica", "Parque Padre Hurtado - La Reina", "12 de septiembre 2026", "https://static.ptocdn.net/images/eventos/glo206_calugalistado.jpg", "https://www.puntoticket.com/piknicelectronik-sept", 35000, 6000),
			evento("After Piknic", "Fiesta Electrónica", "Club Amanda - Vitacura", "12 de septiembre 2026", "https://static.ptocdn.net/images/eventos/glo210_calugalistado.jpg", "https://www.puntoticket.com/evento/after-piknic-amanda", 25000, 1500),
			evento("Deep Purple", "Hard Rock", "Santander Arena - Santiago Centro", "08 de diciembre 2026", "https://static.ptocdn.net/images/eventos/rec112_calugalistado.jpg", "https://www.puntoticket.com/deep-purple", 60000, 10000),
		evento("San Luis - Aunque Sea Todo World Tour 2026", "Baladas", "Teatro Coliseo - Santiago Centro", "12 de septiembre 2026", "https://static.ptocdn.net/images/eventos/lse010_calugalistado.jpg", "https://www.puntoticket.com/evento/Sanluis-santiago-2026", 28000, 2500),
		evento("Romeo Santos y Prince Royce", "Bachata", "Estadio Monumental - Macul", "20 de septiembre 2026", "https://static.ptocdn.net/images/eventos/bsp001_calugalistado.jpg", "https://www.puntoticket.com/romeo-santos-y-prince-royce-2026", 75000, 35000),
			evento("Masters of Dirt", "Exhibición", "Santander Arena - Santiago Centro", "27 de septiembre 2026", "https://static.ptocdn.net/images/eventos/wal277_calugalistado.jpg", "https://www.puntoticket.com/masters-of-dirt-2026", 40000, 9000),
			evento("Morat", "Pop", "Santander Arena - Santiago Centro", "09 de septiembre 2026", "", "https://www.puntoticket.com/morat", 65000, 12000),
			evento("El Unipersonal de Lucho Mellera", "Humor", "Santander Arena - Santiago Centro", "11 de abril 2027", "https://static.ptocdn.net/images/eventos/rds105_calugalistado.jpg", "https://www.puntoticket.com/lucho-mellera", 30000, 8000),
			evento("Drefquila - Diablo Santo 2 Tour", "Especiales", "Teatro Coliseo - Santiago Centro", "08 de noviembre 2026", "https://static.ptocdn.net/images/eventos/bee002_calugalistado.jpg", "https://www.puntoticket.com/evento/drefquila-teatro-coliseo-nov-2026", 45000, 2500),
			evento("Mirko Jozic vuelve a Chile - Siempre Colocolino", "Homenaje", "Teatro Caupolicán - Santiago Centro", "09 de septiembre 2026", "https://static.ptocdn.net/images/eventos/div307_calugalistado.jpg", "https://www.puntoticket.com/evento/homenaje-mirko-josik-teatro-caupolican-sep-2026", 28000, 3000),
			evento("EDO CAROE - Tiene Santander Arena", "Humor", "Santander Arena - Santiago Centro", "27 de noviembre 2026", "https://static.ptocdn.net/images/eventos/edo114v2_calugalistado.jpg", "https://www.puntoticket.com/edocaroe-movistar-arena", 35000, 9000),
			evento("Fede Vigevani", "Youtuber", "Claro Arena - Las Condes", "19 de diciembre 2026", "https://static.ptocdn.net/images/eventos/lot230_calugalistado.jpg", "https://www.puntoticket.com/fede-vigevani-2026", 30000, 10000),
			evento("17va Corrida Por la Vida - Yo Mujer", "Corrida", "Parque Bicentenario de Vitacura", "25 de octubre 2026", "https://static.ptocdn.net/images/eventos/mok005_calugalistado.jpg", "https://www.puntoticket.com/corridaporlavida", 12000, 5000),
			evento("Fonda Oficial Cerrillos", "Fonda", "Parque Don Orione - Cerrillos", "17 de septiembre 2026", "https://static.ptocdn.net/images/eventos/div309v2_calugalistado.jpg", "https://www.puntoticket.com/fonda-oficial-cerrillos-2026", 20000, 10000),
			evento("Martin White", "Urbano", "Teatro Caupolicán - Santiago Centro", "19 de diciembre 2026", "https://static.ptocdn.net/images/eventos/wal284_calugalistado.jpg", "https://www.puntoticket.com/martin-white-teatro-caupolican-2026", 28000, 2500),
			evento("Taburete", "Post Rock", "Teatro Coliseo - Santiago Centro", "04 de diciembre 2026", "https://static.ptocdn.net/images/eventos/wal285_calugalistado.jpg", "https://www.puntoticket.com/taburete-2026", 45000, 3000),
			evento("DAME", "Electrónica", "Ex Clasificadora de Correos - Estación Central", "12 de septiembre 2026", "https://static.ptocdn.net/images/eventos/dam015v2_calugalistado.jpg", "https://www.puntoticket.com/evento/dame-ex-clasificadora-de-correos-sep-2026", 25000, 2500),
			evento("Ha*Ash - No Me Hablen De Amor Tour", "Pop", "Santander Arena - Santiago Centro", "21 de marzo 2027", "https://static.ptocdn.net/images/eventos/biz391_calugalistado.jpg", "https://www.puntoticket.com/ha-ash", 70000, 12000),
			evento("Pixies - en Movistar Arena", "Rock", "Santander Arena - Santiago Centro", "29 de marzo 2027", "https://static.ptocdn.net/images/eventos/fna386_calugalistado.jpg", "https://www.puntoticket.com/pixies", 65000, 12000),
			evento("Oktoberfest 2026", "Festival Cervecero", "Centro de Eventos Múnich - Peñaflor", "10 de septiembre 2026", "https://static.ptocdn.net/images/eventos/fex013v2_calugalistado.jpg", "https://www.puntoticket.com/oktoberfest-2026", 30000, 7000),
			evento("Circo Pastelito y Tachuela Chico - Festival del Circo", "Circo", "Mall Cenco Florida - La Florida", "10 de septiembre 2026", "https://static.ptocdn.net/images/eventos/cir020_calugalistado.jpg", "https://www.puntoticket.com/circo-pastelito", 18000, 4000),
			evento("Anyma", "Fiesta Electrónica", "Espacio Riesco - Huechuraba", "31 de diciembre 2026", "https://static.ptocdn.net/images/eventos/xim001_calugalistado.jpg", "https://www.puntoticket.com/anyma-2026", 85000, 12000),
			evento("Marco Antonio Solis - Gratitud Tour 2026", "Romántico", "Santander Arena - Santiago Centro", "03 de noviembre 2026", "https://static.ptocdn.net/images/eventos/swg170_calugalistado.jpg", "https://www.puntoticket.com/marco-antonio-solis", 75000, 12000),
			evento("Maná - Vivir sin aire Tour", "Pop-Rock", "Estadio Monumental - Macul", "05 de diciembre 2026", "https://static.ptocdn.net/images/eventos/biz383_calugalistado.jpg", "https://www.puntoticket.com/mana", 80000, 40000),
			evento("Anuel AA - World Tour", "Reggaetón", "Santander Arena - Santiago Centro", "13 de diciembre 2026", "https://static.ptocdn.net/images/eventos/jts123_calugalistado.jpg", "https://www.puntoticket.com/anuel-aa-world-tour", 70000, 12000),
			evento("Jamiroquai", "Jazz-Rock", "Claro Arena - Las Condes", "15 de septiembre 2026", "https://static.ptocdn.net/images/eventos/lot221_calugalistado.jpg", "https://www.puntoticket.com/jamiroquai-2026", 65000, 15000),
			evento("Festival Fauna Primavera 2026", "Festival", "Parque Ciudad Empresarial - Huechuraba", "26 de noviembre 2026", "https://static.ptocdn.net/images/eventos/fna377_calugalistado.jpg", "https://www.puntoticket.com/faunaprimavera", 90000, 30000),
			evento("Karol G - Viajando por el Mundo TropiTour", "Urbano", "Estadio Nacional - Ñuñoa", "28 de enero 2027", "https://static.ptocdn.net/images/eventos/biz378_calugalistado.jpg", "https://www.puntoticket.com/karol-g", 95000, 45000),
			evento("Solomun", "Fiesta", "Espacio Riesco - Huechuraba", "30 de octubre 2026", "https://static.ptocdn.net/images/eventos/sm0195_calugalistado.jpg", "https://www.puntoticket.com/solomun-2026", 60000, 10000),
			evento("Los Tres - Unplugged 30 años", "Rock", "Santander Arena - Santiago Centro", "14 de noviembre 2026", "https://static.ptocdn.net/images/eventos/rec115_calugalistado.jpg", "https://www.puntoticket.com/los-tres", 55000, 12000),
			evento("Soy Luna - El Último show", "Infantil", "Santander Arena - Santiago Centro", "22 de noviembre 2026", "https://static.ptocdn.net/images/eventos/wal281_calugalistado.jpg", "https://www.puntoticket.com/soy-luna-ultimo-show-2026", 40000, 10000),
			evento("David Bisbal - Tour Eternos 2026", "Pop", "Santander Arena - Santiago Centro", "06 de octubre 2026", "https://static.ptocdn.net/images/eventos/ft0106_calugalistado.jpg", "https://www.puntoticket.com/david-bisbal", 65000, 12000),
			evento("Respira Primavera 2026", "Techno", "Espacio Riesco - Huechuraba", "21 de noviembre 2026", "https://static.ptocdn.net/images/eventos/sou068_calugalistado.jpg", "https://www.puntoticket.com/festival-primavera-2026", 70000, 15000),
			evento("Aitana", "Pop", "Santander Arena - Santiago Centro", "19 de octubre 2026", "https://static.ptocdn.net/images/eventos/biz375_calugalistado.jpg", "https://www.puntoticket.com/aitana", 70000, 12000)
		);
	}

	private Evento evento(String nombre, String categoria, String lugar, String fecha, String imagenUrl, String urlEvento, int precio, int capacidad) {
		LocalDateTime fechaInicio = fechaInicio(fecha);
		return new Evento(null, nombre, "Disfruta de " + nombre + " en GoTicket.", categoria, imagenUrl, urlEvento,
				fechaInicio, fechaInicio.plusHours(4), lugar, capacidad, BigDecimal.valueOf(precio), true);
	}

	private LocalDateTime fechaInicio(String fecha) {
		String[] partes = fecha.split(" ");
		int dia = Integer.parseInt(partes[0]);
		int mes = switch (partes[2].toLowerCase()) {
			case "enero" -> 1; case "febrero" -> 2; case "marzo" -> 3; case "abril" -> 4;
			case "mayo" -> 5; case "junio" -> 6; case "julio" -> 7; case "agosto" -> 8;
			case "septiembre" -> 9; case "octubre" -> 10; case "noviembre" -> 11; case "diciembre" -> 12;
			default -> throw new IllegalArgumentException("Mes no soportado: " + partes[2]);
		};
		return LocalDateTime.of(Integer.parseInt(partes[3]), mes, dia, 20, 0);
	}

}
