package eu.binarysystem.logishift.utilities

import eu.binarysystem.logishift.hilt.SharedPreferencesRetriever
import eu.binarysystem.logishift.utilities.Constants.Companion.SHARED_KEY_AUTH_TOKEN
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthParameterInterceptor(private var pref: SharedPreferencesRetriever) : Interceptor {
    private var authToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        authToken = pref.getSharedManagerInstance().getString(SHARED_KEY_AUTH_TOKEN, null)

        if (authToken == null) {
            return chain.proceed(request)
        }

        val url: HttpUrl = chain.request().url().newBuilder().addQueryParameter("auth_token", authToken).build()

        val nextRequest: Request = chain.request().newBuilder().addHeader("auth_token", authToken!!).addHeader("X-AUTH-TOKEN", authToken!!).url(url).build()

        return chain.proceed(nextRequest)

    }

}