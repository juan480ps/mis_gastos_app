package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel

/**
 * Politica de privacidad, terminos de servicio y borrado de cuenta.
 *
 * Redactado con foco en el marco legal paraguayo (Constitucion Nacional, Ley 1682/2001 modificada
 * por Ley 1969/2002, Ley 1334/98 de Defensa del Consumidor, Ley 4868/2013 de Comercio Electronico,
 * Codigo Civil) y en los marcos internacionales mas exigentes (RGPD/UE, CCPA/California, LGPD/Brasil)
 * dado que la app se publica sin restriccion de pais. No reemplaza la validacion final de un
 * abogado matriculado en Paraguay antes de publicar (ver aviso al principio de la pantalla):
 * ningun texto generado fuera de una consulta profesional formal deberia tratarse como definitivo.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    navController: NavController,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal y privacidad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Aviso: el texto fue elaborado con foco en la normativa aplicable, pero sigue
            // recomendandose una validacion final de un abogado matriculado antes de publicar,
            // sobre todo si el modelo de negocio cambia (ej. se agrega sync en la nube).
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Este documento fue elaborado con foco en la normativa paraguaya e " +
                            "internacional aplicable a esta app. Se recomienda una validación " +
                            "final de un abogado matriculado en Paraguay antes de publicar, en " +
                            "especial si el modelo de negocio cambia (por ejemplo, si en el " +
                            "futuro se agrega sincronización en la nube).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Política de Privacidad")
            Text(
                "Última actualización: agosto de 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            LegalParagraph(
                "My Expenses (\"la App\") es una aplicación de gestión de finanzas personales. " +
                    "Esta política describe qué datos trata la App, con qué finalidad, dónde se " +
                    "guardan y qué derechos tenés sobre ellos, conforme a la Constitución " +
                    "Nacional del Paraguay (arts. 33, 36 y 135), la Ley N° 1682/2001 (modificada " +
                    "por la Ley N° 1969/2002) y, en la medida en que resulten aplicables según tu " +
                    "país de residencia, el Reglamento General de Protección de Datos de la Unión " +
                    "Europea (RGPD), la California Consumer Privacy Act (CCPA/CPRA) y la Lei Geral " +
                    "de Proteção de Dados de Brasil (LGPD)."
            )

            SubsectionTitle("Responsable del tratamiento")
            LegalParagraph(
                "El desarrollador de My Expenses (\"el Desarrollador\"), con domicilio en la " +
                    "República del Paraguay, es responsable de las decisiones sobre el " +
                    "tratamiento de datos descripto en esta política. Podés contactarlo en " +
                    "juan.correa480@gmail.com."
            )

            SubsectionTitle("Paraguay aún no cuenta con una autoridad de protección de datos")
            LegalParagraph(
                "A la fecha de esta política, Paraguay no tiene una ley general de protección de " +
                    "datos personales equivalente al RGPD ni una autoridad de control dedicada. " +
                    "El marco vigente combina la garantía constitucional de hábeas data (art. 135 " +
                    "CN, que te permite acceder, actualizar, rectificar o hacer destruir datos " +
                    "tuyos en cualquier registro), la Ley N° 1682/2001 sobre información de " +
                    "carácter privado, y los principios generales de buena fe e intimidad del " +
                    "Código Civil. Por eso esta política adopta voluntariamente estándares más " +
                    "exigentes (RGPD/CCPA/LGPD) como buena práctica, no porque una ley paraguaya " +
                    "lo exija punto por punto."
            )

            SubsectionTitle("Qué datos trata la App y por qué")
            LegalParagraph(
                "• Datos financieros (transacciones, presupuestos, cuentas, categorías): se " +
                    "guardan únicamente en este dispositivo, en una base de datos cifrada " +
                    "(SQLCipher, con clave protegida por Android Keystore). La App no tiene " +
                    "servidor propio: esta información nunca sale del teléfono."
            )
            LegalParagraph(
                "• Cuenta local (opcional): si creás una cuenta con email y contraseña, o " +
                    "iniciás sesión con Google, tu nombre, email y contraseña (cifrada) se " +
                    "guardan también solo en este dispositivo, con la única finalidad de " +
                    "identificarte dentro de la App instalada en tu teléfono."
            )
            LegalParagraph(
                "• Publicidad (Google AdMob): en la versión gratuita, Google actúa como " +
                    "responsable independiente del tratamiento de tu identificador de " +
                    "publicidad y datos de uso asociados a los anuncios, conforme a su propia " +
                    "política (policies.google.com/privacy). La App no le comparte tus datos " +
                    "financieros ni de cuenta a AdMob."
            )
            LegalParagraph(
                "• Compras (Google Play Billing): el pago de Premium (USD 0,99, pago único) lo " +
                    "procesa Google Play como responsable independiente. La App solo guarda " +
                    "localmente si tenés Premium activo."
            )
            LegalParagraph(
                "• Base legal: tu consentimiento, otorgado al crear una cuenta, iniciar sesión " +
                    "con Google, o al usar los servicios de anuncios/compras de Google."
            )

            SubsectionTitle("Transferencias internacionales")
            LegalParagraph(
                "Tus datos financieros y de cuenta no se transfieren a ningún país porque nunca " +
                    "salen de tu dispositivo. Los datos que sí procesa Google (AdMob, Billing, " +
                    "Sign-In) pueden tratarse en servidores fuera de Paraguay, bajo las garantías " +
                    "y mecanismos de transferencia internacional propios de Google (cláusulas " +
                    "contractuales tipo u otros mecanismos reconocidos por el RGPD)."
            )

            SubsectionTitle("Tus derechos (ARCO / hábeas data / RGPD)")
            LegalParagraph(
                "Independientemente de tu país de residencia, podés ejercer en cualquier momento " +
                    "los derechos de Acceso, Rectificación, Cancelación y Oposición (\"ARCO\"), " +
                    "equivalentes a los reconocidos por el hábeas data constitucional paraguayo " +
                    "(art. 135 CN) y, cuando corresponda, a los derechos de acceso, rectificación, " +
                    "supresión, portabilidad, limitación y oposición del RGPD, o a tus derechos " +
                    "bajo la CCPA/LGPD. En esta App, la mayoría de estos derechos ya están " +
                    "satisfechos por diseño: vos tenés control físico y exclusivo de tus datos en " +
                    "tu dispositivo, y podés corregir o borrar cualquier dato editando o " +
                    "eliminando la transacción, cuenta o categoría correspondiente, o eliminando " +
                    "tu cuenta por completo (ver \"Tu cuenta y tus datos\" más abajo)."
            )
            LegalParagraph(
                "Para cualquier consulta sobre tus datos que no puedas resolver desde la App " +
                    "(por ejemplo, dudas sobre el tratamiento que hace Google), escribí a " +
                    "juan.correa480@gmail.com. Vamos a responder en un plazo razonable, no mayor " +
                    "a 30 días corridos."
            )
            LegalParagraph(
                "Para gestionar los anuncios personalizados de Google directamente, podés " +
                    "visitar adssettings.google.com o restablecer tu identificador de publicidad " +
                    "desde los ajustes de privacidad de tu dispositivo Android."
            )

            SubsectionTitle("Lo que esta App no hace")
            LegalParagraph(
                "No vende tus datos personales (en el sentido de la CCPA, esta App no realiza " +
                    "\"sale\" ni \"sharing\" de información personal a cambio de valor). No " +
                    "comparte tus datos con terceros más allá de lo descripto arriba. No accede " +
                    "a tu ubicación, contactos, cámara ni micrófono. No usa servicios de análisis " +
                    "o rastreo propios."
            )

            SubsectionTitle("Seguridad y conservación")
            LegalParagraph(
                "Los datos sensibles (base de datos financiera, sesión y contraseña) se guardan " +
                    "cifrados en el dispositivo mediante SQLCipher y Android Keystore. Se " +
                    "conservan mientras uses la App o mantengas tu cuenta activa; si eliminás tu " +
                    "cuenta, se borran de inmediato y en su totalidad de este dispositivo. Ningún " +
                    "sistema es 100% infalible: te recomendamos proteger tu teléfono con PIN o " +
                    "biometría."
            )

            SubsectionTitle("Menores de edad")
            LegalParagraph(
                "Esta App no está dirigida a menores de 13 años (o la edad mínima de " +
                    "consentimiento digital que exija tu país de residencia, que en algunos " +
                    "estados miembro de la UE puede llegar a 16 años) y no recopila " +
                    "intencionalmente datos de menores de esa edad."
            )

            SubsectionTitle("Cambios a esta política")
            LegalParagraph(
                "Si esta política cambia de forma significativa, se actualizará esta pantalla y " +
                    "se avisará dentro de la App antes de que el cambio entre en vigencia."
            )

            SubsectionTitle("Contacto")
            LegalParagraph(
                "Ante cualquier duda sobre tus datos o esta política, escribí a " +
                    "juan.correa480@gmail.com."
            )

            Spacer(modifier = Modifier.height(28.dp))
            SectionTitle("Términos de Servicio")
            Text(
                "Última actualización: agosto de 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            LegalParagraph(
                "Estos términos constituyen un contrato de adhesión entre vos y el Desarrollador " +
                    "de My Expenses. Al descargar, instalar o usar la App aceptás estos términos " +
                    "en su totalidad; si no estás de acuerdo, no debés usar la App. Se rigen, en " +
                    "lo pertinente, por la Ley N° 4868/2013 de Comercio Electrónico (validez de " +
                    "la aceptación electrónica), la Ley N° 1334/98 de Defensa del Consumidor y el " +
                    "Código Civil paraguayo (Ley N° 1183/1985), sin perjuicio de los derechos " +
                    "irrenunciables que la ley de tu país de residencia te reconozca como " +
                    "consumidor."
            )

            SubsectionTitle("El servicio")
            LegalParagraph(
                "My Expenses te permite registrar y organizar tus gastos e ingresos personales, " +
                    "de forma local en tu dispositivo. La versión gratuita tiene límites de uso " +
                    "(categorías, cuentas, presupuestos, historial, etc.); la versión Premium " +
                    "(USD 0,99, pago único, sin vencimiento) los elimina y quita la publicidad."
            )

            SubsectionTitle("Edad mínima")
            LegalParagraph(
                "Para usar la App declarás tener al menos 13 años, o la edad mínima que exija tu " +
                    "país de residencia para aceptar estos términos sin autorización de un " +
                    "adulto responsable."
            )

            SubsectionTitle("Licencia de uso")
            LegalParagraph(
                "El Desarrollador te otorga una licencia limitada, personal, no exclusiva, " +
                    "intransferible y revocable para instalar y usar la App en tus propios " +
                    "dispositivos, exclusivamente para tu uso personal y no comercial. La App, su " +
                    "código, diseño, marca y contenidos son propiedad del Desarrollador o de sus " +
                    "licenciantes (incluidas las bibliotecas de terceros usadas bajo sus propias " +
                    "licencias de código abierto); esta licencia no te transfiere ningún derecho " +
                    "de propiedad intelectual sobre ellos."
            )

            SubsectionTitle("Tu cuenta")
            LegalParagraph(
                "Crear una cuenta es opcional. Si lo hacés, sos responsable de mantener tu " +
                    "contraseña a salvo y de toda actividad realizada desde tu dispositivo. Podés " +
                    "eliminar tu cuenta y todos tus datos en cualquier momento desde esta " +
                    "pantalla (ver \"Tu cuenta y tus datos\" más abajo)."
            )

            SubsectionTitle("Uso permitido")
            LegalParagraph(
                "Te comprometés a no realizar ingeniería inversa, descompilar ni modificar la " +
                    "App salvo en la medida en que la ley aplicable lo permita expresamente, y a " +
                    "no usarla con fines ilícitos o para vulnerar derechos de terceros."
            )

            SubsectionTitle("Compras, precios y reembolsos")
            LegalParagraph(
                "Las compras dentro de la App se procesan íntegramente a través de Google Play " +
                    "Billing y se rigen por las políticas de pago y reembolso de Google Play, no " +
                    "por el Desarrollador. Los precios pueden incluir impuestos locales según tu " +
                    "país. Esto no afecta ningún derecho irrenunciable de reembolso o " +
                    "desistimiento que la legislación de consumo de tu país de residencia te " +
                    "reconozca de forma imperativa."
            )

            SubsectionTitle("Ausencia de asesoramiento financiero")
            LegalParagraph(
                "La App es una herramienta de organización personal, no un servicio de " +
                    "asesoría financiera, contable, impositiva ni de inversión. Las decisiones " +
                    "que tomes en base a la información que registrás son de tu exclusiva " +
                    "responsabilidad."
            )

            SubsectionTitle("Garantías y limitación de responsabilidad")
            LegalParagraph(
                "La App se ofrece \"tal cual\" y \"según disponibilidad\", sin garantía de que " +
                    "esté libre de errores, ininterrumpida o de que los cálculos, reportes o " +
                    "exportaciones sean exactos. En la máxima medida permitida por la ley " +
                    "aplicable, el Desarrollador no será responsable por daños indirectos, " +
                    "incidentales o consecuentes derivados del uso de la App, ni por decisiones " +
                    "financieras tomadas en base a ella; en ningún caso su responsabilidad total " +
                    "superará el monto efectivamente pagado por vos por Premium en los últimos 12 " +
                    "meses. Nada en esta cláusula excluye responsabilidad que no pueda limitarse " +
                    "válidamente bajo la ley aplicable (por ejemplo, por dolo o culpa grave, o " +
                    "derechos irrenunciables del consumidor)."
            )

            SubsectionTitle("Terminación")
            LegalParagraph(
                "Podés dejar de usar la App o desinstalarla, y eliminar tu cuenta, en cualquier " +
                    "momento. El Desarrollador podrá discontinuar o modificar la App con un aviso " +
                    "razonable cuando las circunstancias lo permitan."
            )

            SubsectionTitle("Divisibilidad y modificaciones")
            LegalParagraph(
                "Si alguna cláusula de estos términos fuera declarada inválida, el resto " +
                    "continuará vigente. Estos términos pueden actualizarse; los cambios " +
                    "significativos se avisarán dentro de la App antes de entrar en vigencia."
            )

            SubsectionTitle("Ley aplicable y jurisdicción")
            LegalParagraph(
                "Estos términos se rigen por las leyes de la República del Paraguay. Cualquier " +
                    "controversia se someterá a los tribunales ordinarios de la ciudad de " +
                    "Asunción, Paraguay, sin perjuicio de los derechos imperativos que, como " +
                    "consumidor, te reconozca de forma irrenunciable la legislación de tu país de " +
                    "residencia (por ejemplo, el derecho a litigar ante los tribunales de tu " +
                    "propio domicilio cuando esa legislación así lo exija). Antes de iniciar " +
                    "cualquier reclamo formal, te pedimos que nos escribas a " +
                    "juan.correa480@gmail.com para intentar resolverlo directamente."
            )

            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle("Tu cuenta y tus datos")
            Spacer(modifier = Modifier.height(12.dp))

            LegalParagraph(
                "El botón de abajo ejerce tu derecho de cancelación/supresión (parte de los " +
                    "derechos ARCO y del hábeas data constitucional, art. 135 CN, y equivalente " +
                    "al derecho de supresión del RGPD): borra de inmediato tu cuenta y todos los " +
                    "datos guardados en este dispositivo."
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isLoggedIn) {
                        Text(
                            "Eliminar tu cuenta borra permanentemente tu perfil y TODOS los " +
                                "datos guardados en este dispositivo: transacciones, " +
                                "presupuestos, cuentas y categorías. Esta acción no se puede " +
                                "deshacer.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar mi cuenta")
                        }
                    } else {
                        Text(
                            "No tenés una cuenta activa en este dispositivo, así que no hay " +
                                "nada que eliminar por ahora.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showDeleteConfirmation = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Text(
                    "Se borrará tu cuenta y TODOS tus datos financieros de este dispositivo " +
                        "de forma permanente. No hay forma de deshacer esto."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        authViewModel.deleteAccount(
                            onSuccess = {
                                showDeleteConfirmation = false
                                Toast.makeText(
                                    context,
                                    "Tu cuenta y tus datos fueron eliminados.",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isLoading) "Eliminando…" else "Sí, eliminar todo")
                }
            },
            dismissButton = {
                TextButton(enabled = !isLoading, onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun SubsectionTitle(title: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
}
