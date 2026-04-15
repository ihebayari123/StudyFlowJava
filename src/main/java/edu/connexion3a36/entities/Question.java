package edu.connexion3a36.entities;

public class Question {

    private int     id;
    private String  texte;
    private String  niveau;      // facile | moyen | difficile
    private String  indice;
    private int     quizId;
    private String  type;        // choix_multiple | vrai_faux | texte

    // Choix multiple
    private String  choixA, choixB, choixC, choixD;
    private String  bonneReponseChoix;   // "a" | "b" | "c" | "d"

    // Vrai / Faux
    private Boolean bonneReponseBool;

    // Texte libre
    private String  reponseAttendue;

    public Question() {}

    // ── Getters & Setters ──────────────────────────────────────
    public int     getId()                           { return id; }
    public void    setId(int id)                     { this.id = id; }
    public String  getTexte()                        { return texte; }
    public void    setTexte(String t)                { this.texte = t; }
    public String  getNiveau()                       { return niveau; }
    public void    setNiveau(String n)               { this.niveau = n; }
    public String  getIndice()                       { return indice; }
    public void    setIndice(String i)               { this.indice = i; }
    public int     getQuizId()                       { return quizId; }
    public void    setQuizId(int q)                  { this.quizId = q; }
    public String  getType()                         { return type; }
    public void    setType(String t)                 { this.type = t; }
    public String  getChoixA()                       { return choixA; }
    public void    setChoixA(String c)               { this.choixA = c; }
    public String  getChoixB()                       { return choixB; }
    public void    setChoixB(String c)               { this.choixB = c; }
    public String  getChoixC()                       { return choixC; }
    public void    setChoixC(String c)               { this.choixC = c; }
    public String  getChoixD()                       { return choixD; }
    public void    setChoixD(String c)               { this.choixD = c; }
    public String  getBonneReponseChoix()            { return bonneReponseChoix; }
    public void    setBonneReponseChoix(String b)    { this.bonneReponseChoix = b; }
    public Boolean getBonneReponseBool()             { return bonneReponseBool; }
    public void    setBonneReponseBool(Boolean b)    { this.bonneReponseBool = b; }
    public String  getReponseAttendue()              { return reponseAttendue; }
    public void    setReponseAttendue(String r)      { this.reponseAttendue = r; }

    @Override
    public String toString() { return "[" + type + "] " + texte; }
}
