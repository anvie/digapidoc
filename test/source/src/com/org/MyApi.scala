package com.org


class MyApi {


    /**
     * GET /user/{USER-ID-OR-NAME}
     *
     * Mendapatkan informasi user.
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     *
     * + Parameters:
     *
     *      + ref - reference id.
     */
    def getUserInfo() = {
        // @TODO(you): code here
    }

    /**
     * DELETE /user/{USER-ID-OR-NAME}
     *
     * Untuk menghapus user.
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     */
    def deleteUser() = {
        // @TODO(you): code here
    }

    /**
     * PUT /user/{USER-ID-OR-NAME}
     *
     * Untuk meng-update user
     *
     * + Symbols:
     *
     *      + {USER-ID-OR-NAME} - id user atau nama user.
     *
     * + Parameters:
     *
     *      + full_name=`` - user full name.
     *      + birth_date=`` - user birth date.
     *      + email - user email.
     **/
    def updateUser() = {

    }


}